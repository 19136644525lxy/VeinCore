package com.veincore.fabric;

import com.mojang.authlib.GameProfile;
import com.veincore.auth.AccessManager;
import com.veincore.config.JsonStore;
import com.veincore.config.ServerConfig;
import com.veincore.core.BlockGroupIndex;
import com.veincore.core.BreakPlan;
import com.veincore.core.Cooldown;
import com.veincore.core.Face;
import com.veincore.core.MiningEngine;
import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;
import com.veincore.core.pattern.PatternContext;
import com.veincore.core.pattern.PatternId;
import com.veincore.platform.IEventRegistrar;
import com.veincore.platform.IProtectionProbe;
import com.veincore.platform.PlayerHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 服务端连锁核心（M2）：热键状态 → 权限校验 → 破坏事件 → 按玩家模式扫描 → 耐久/饱食度 → 接管破坏。
 */
public final class VeinCoreService {

    private final FabricPlatform platform;
    private final ServerConfig config;
    private final MiningEngine engine;
    private final AccessManager access;
    private final BlockGroupIndex groups;
    private final IProtectionProbe protection = new FabricProtectionProbe();
    private final Set<UUID> pressed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Cooldown> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, PatternId> patterns = new ConcurrentHashMap<>();

    public VeinCoreService(FabricPlatform platform) {
        this.platform = platform;
        this.config = loadConfig(platform.configDir());
        this.engine = new MiningEngine(config);
        this.access = loadAccess(platform.configDir());
        this.groups = BlockGroupIndex.from(config.blockGroups);
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

    /** 记录玩家当前选择的模式；未知 key 回退服务端默认。 */
    public void setPattern(UUID id, String key) {
        PatternId p = PatternId.byKey(key);
        if (p == null) {
            patterns.remove(id);
        } else {
            patterns.put(id, p);
        }
    }

    public boolean isPressed(UUID id) {
        return pressed.contains(id);
    }

    /** 玩家离开：清理热键、冷却与模式状态。 */
    public void forget(PlayerHandle handle) {
        UUID id = UUID.fromString(handle.id());
        pressed.remove(id);
        cooldowns.remove(id);
        patterns.remove(id);
    }

    /** owner = 单机/局域网主机，或 OP 等级 ≥ 4。控制台在命令层处理。 */
    public boolean isOwner(ServerPlayer player) {
        MinecraftServer server = platform.server();
        GameProfile host = server.getSingleplayerProfile();
        if (host != null && host.getId() != null && host.getId().equals(player.getUUID())) {
            return true;
        }
        return player.hasPermissions(4);
    }

    public boolean isAllowed(UUID id) {
        return access.isAllowed(id.toString());
    }

    public AccessManager access() {
        return access;
    }

    /** 权限变更后写回 access 文件；写失败不阻断命令。 */
    public void saveAccess() {
        try {
            JsonStore.save(platform.configDir().resolve("veincore-access.json"), access.toMap());
        } catch (IOException ignored) {
            // 忽略：下次启动仍可重试
        }
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
        if (!authorized(player)) {
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
        // 同组方块（如 iron_ore 与 deepslate_iron_ore）也视为可连锁
        Predicate<Pos> sameGroup = p -> groups.sameGroup(originBlock, blockId(level.getBlockState(blockPos(p))));
        PatternContext ctx = new PatternContext(pos, face, axis, config.maxBlocks, canMine, sameGroup);
        PatternId selected = patterns.get(id);
        String patternKey = selected == null ? config.defaultPattern : selected.key;
        BreakPlan plan = engine.plan(patternKey, ctx);
        if (plan.over600()) {
            player.sendSystemMessage(Component.literal(
                    "VeinCore: 本次连锁 " + plan.size() + " 格（>600），卡顿由你自行承担。"));
        }
        // 先挖玩家点的原方块，再逐格连锁；每挖一格扣一次耐久，工具损毁立即停止。
        // 工具剩余耐久不足以覆盖整个计划时，只挖耐久够的那几格。
        platform.requestBreak(handle, pos);
        if (config.hungerPerBlock > 0) {
            player.causeFoodExhaustion(config.hungerPerBlock);
        }
        if (!consumeToolOnce(player)) {
            return false;
        }
        for (Pos p : plan.blocks()) {
            if (p.equals(pos)) {
                continue;
            }
            platform.requestBreak(handle, p);
            if (config.hungerPerBlock > 0) {
                player.causeFoodExhaustion(config.hungerPerBlock);
            }
            if (!consumeToolOnce(player)) {
                break;
            }
        }
        return false;
    }

    /** 每破坏一格消耗一次耐久（原版 hurtAndBreak 内含耐久附魔减免）；工具损毁返回 false。 */
    private boolean consumeToolOnce(ServerPlayer player) {
        if (player.isCreative()) {
            return true;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return true;
        }
        stack.hurtAndBreak(config.durabilityPerBlock, player.serverLevel(), player, item -> {
        });
        return !player.getMainHandItem().isEmpty();
    }

    /** 权限：单机主机、OP≥4、或 access 列表中的玩家。 */
    private boolean authorized(ServerPlayer player) {
        return isOwner(player) || access.isAllowed(player.getUUID().toString());
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

    private static AccessManager loadAccess(Path configDir) {
        Path file = configDir.resolve("veincore-access.json");
        return AccessManager.fromMap(JsonStore.load(file, new AccessManager().toMap()));
    }
}
