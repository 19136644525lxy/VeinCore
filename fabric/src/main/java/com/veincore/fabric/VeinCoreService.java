package com.veincore.fabric;

import com.veincore.config.JsonStore;
import com.veincore.config.ServerConfig;
import com.veincore.core.BreakPlan;
import com.veincore.core.Cooldown;
import com.veincore.core.Face;
import com.veincore.core.MiningEngine;
import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;
import com.veincore.core.pattern.PatternContext;
import com.veincore.platform.IEventRegistrar;
import com.veincore.platform.IProtectionProbe;
import com.veincore.platform.PlayerHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 服务端连锁核心（M1）：热键状态 → 破坏事件 → 模式扫描 → 接管破坏。
 *
 * <p>M1 范围：单机/局域网可用；权限（M2）、预览/指示器（M4）未接入。</p>
 */
public final class VeinCoreService {

    private final FabricPlatform platform;
    private final ServerConfig config;
    private final MiningEngine engine;
    private final IProtectionProbe protection = new FabricProtectionProbe();
    private final Set<UUID> pressed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Cooldown> cooldowns = new ConcurrentHashMap<>();

    public VeinCoreService(FabricPlatform platform) {
        this.platform = platform;
        this.config = loadConfig(platform.configDir());
        this.engine = new MiningEngine(config);
    }

    /** 注册到平台事件。 */
    public void register(IEventRegistrar events) {
        events.onPlayerBreakBlock(this::handleBreak);
    }

    public void setPressed(UUID id, boolean value) {
        if (value) {
            pressed.add(id);
        } else {
            pressed.remove(id);
        }
    }

    public boolean isPressed(UUID id) {
        return pressed.contains(id);
    }

    /** 玩家离开：清理热键与冷却状态。 */
    public void forget(PlayerHandle handle) {
        UUID id = UUID.fromString(handle.id());
        pressed.remove(id);
        cooldowns.remove(id);
    }

    /** 返回 true = 放行原破坏；false = 连锁接管（原方块也由本类破坏）。 */
    public boolean handleBreak(PlayerHandle handle, Pos pos) {
        UUID id = UUID.fromString(handle.id());
        if (!pressed.contains(id)) {
            return true;
        }
        ServerPlayer player = platform.playerById(handle.id());
        if (player == null) {
            return true;
        }
        Cooldown cooldown = cooldowns.computeIfAbsent(id, k -> new Cooldown(config.cooldownTicks));
        if (!cooldown.ready(platform.serverTick())) {
            return true;
        }
        ServerLevel level = player.serverLevel();
        BlockPos origin = new BlockPos(pos.x(), pos.y(), pos.z());
        BlockState originState = level.getBlockState(origin);
        if (originState.isAir()) {
            return true;
        }
        Vec3 look = player.getViewVector(1.0f);
        ViewAxis axis = ViewAxis.fromLook(look.x, look.y, look.z);
        Face face = faceFor(look);
        String originBlock = blockId(originState);
        Predicate<Pos> canMine = p -> {
            BlockPos bp = blockPos(p);
            BlockState st = level.getBlockState(bp);
            if (st.isAir() || !st.getFluidState().isEmpty()) {
                return false;
            }
            if (st.getDestroySpeed(level, bp) < 0) {
                return false;
            }
            if (config.blacklist.contains(blockId(st))) {
                return false;
            }
            return protection.canBreak(handle, p);
        };
        Predicate<Pos> sameGroup = p -> blockId(level.getBlockState(blockPos(p))).equals(originBlock);
        PatternContext ctx = new PatternContext(pos, face, axis, config.maxBlocks, canMine, sameGroup);
        BreakPlan plan = engine.plan(config.defaultPattern, ctx);
        if (plan.over600()) {
            player.sendSystemMessage(Component.literal(
                    "VeinCore: 本次连锁 " + plan.size() + " 格（>600），卡顿由你自行承担。"));
        }
        for (Pos p : plan.blocks()) {
            if (p.equals(pos)) {
                continue;
            }
            platform.requestBreak(handle, p);
        }
        // 原方块由事件取消，这里统一破坏（保持掉落与工具逻辑一致）
        platform.requestBreak(handle, pos);
        return false;
    }

    private static BlockPos blockPos(Pos p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static Face faceFor(Vec3 look) {
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);
        if (ay >= ax && ay >= az) {
            return look.y < 0 ? Face.UP : Face.DOWN;
        }
        if (ax >= az) {
            return look.x < 0 ? Face.EAST : Face.WEST;
        }
        return look.z < 0 ? Face.SOUTH : Face.NORTH;
    }

    private static ServerConfig loadConfig(Path configDir) {
        Path file = configDir.resolve("veincore-server.json");
        Map<String, Object> defaults = new ServerConfig().toMap();
        return ServerConfig.fromMap(JsonStore.load(file, defaults));
    }
}
