package com.veincore.forge;

import com.mojang.authlib.GameProfile;
import com.veincore.auth.AccessManager;
import com.veincore.config.JsonStore;
import com.veincore.config.MiniJson;
import com.veincore.config.ServerConfig;
import com.veincore.core.BlockGroupIndex;
import com.veincore.core.BreakPlan;
import com.veincore.core.Cooldown;
import com.veincore.core.DropMerger;
import com.veincore.core.Face;
import com.veincore.core.MaxBlocksLimiter;
import com.veincore.core.MiningEngine;
import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;
import com.veincore.core.pattern.PatternContext;
import com.veincore.core.pattern.PatternId;
import com.veincore.data.PlayerSettings;
import com.veincore.platform.IProtectionProbe;
import com.veincore.platform.PlayerHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Forge 服务端连锁核心（M3）：热键状态 → 权限校验 → 破坏事件 → 按玩家模式扫描 → 耐久/饱食度 → 接管破坏。
 *
 * <p>M3 起：玩家个人上限与模式持久化；数据包可覆盖方块组；统计；配置热重载。</p>
 */
public final class VeinCoreForgeService {

    private static final String PLAYERS_FILE = "veincore-players.json";
    private static final long SAVE_INTERVAL_TICKS = 100;

    private final ForgePlatform platform;
    private final IProtectionProbe protection = new ForgeProtectionProbe();
    private final Set<UUID> pressed = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Cooldown> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, PatternId> patterns = new ConcurrentHashMap<>();

    private ServerConfig config;
    private MiningEngine engine;
    private AccessManager access;
    private BlockGroupIndex groups;
    private PlayerSettings players;
    private long lastSaveTick = -SAVE_INTERVAL_TICKS;

    public VeinCoreForgeService(ForgePlatform platform) {
        this.platform = platform;
        this.config = loadConfig(platform.configDir());
        this.engine = new MiningEngine(config);
        this.access = loadAccess(platform.configDir());
        this.players = PlayerSettings.load(platform.configDir().resolve(PLAYERS_FILE));
        this.groups = BlockGroupIndex.from(config.blockGroups);
    }

    public void setPressed(UUID id, boolean value) {
        if (value) {
            pressed.add(id);
        } else {
            pressed.remove(id);
        }
    }

    /** 记录玩家当前选择的模式并持久化。 */
    public void setPattern(UUID id, String key) {
        PatternId p = PatternId.byKey(key);
        if (p == null) {
            patterns.remove(id);
            players.setPattern(id.toString(), null);
        } else {
            patterns.put(id, p);
            players.setPattern(id.toString(), p.key);
        }
        savePlayers();
    }

    public void forget(PlayerHandle handle) {
        UUID id = UUID.fromString(handle.id());
        pressed.remove(id);
        cooldowns.remove(id);
        patterns.remove(id);
    }

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

    public ServerConfig config() {
        return config;
    }

    public BlockGroupIndex groups() {
        return groups;
    }

    public PlayerSettings playerSettings() {
        return players;
    }

    /** 当前生效的挖掘上限：个人设置（缺省为服务器默认）再与服务器封顶夹取。 */
    public int maxBlocksFor(UUID id) {
        Integer personal = players.maxBlocks(id.toString());
        int base = personal == null ? config.defaultPlayerBlocks : personal;
        return MaxBlocksLimiter.effective(base, config.maxBlocks);
    }

    public void saveAccess() {
        try {
            JsonStore.save(platform.configDir().resolve("veincore-access.json"), access.toMap());
        } catch (IOException ignored) {
            // 忽略：下次启动仍可重试
        }
    }

    public void savePlayers() {
        try {
            players.save(platform.configDir().resolve(PLAYERS_FILE));
        } catch (IOException ignored) {
            // 忽略：下次启动仍可重试
        }
    }

    private void savePlayersThrottled() {
        long now = platform.serverTick();
        if (now - lastSaveTick < SAVE_INTERVAL_TICKS) {
            return;
        }
        lastSaveTick = now;
        savePlayers();
    }

    /** 热重载 server/access/players 配置与数据包方块组。 */
    public void reload(ResourceManager resources) {
        this.config = loadConfig(platform.configDir());
        this.engine = new MiningEngine(config);
        this.access = loadAccess(platform.configDir());
        this.players = PlayerSettings.load(platform.configDir().resolve(PLAYERS_FILE));
        this.groups = BlockGroupIndex.from(config.blockGroups);
        loadDataPack(resources);
    }

    /** 读取数据包方块组：data/<ns>/veinminer/groups/<组名>.json，内容 {"blocks":[...]}。 */
    public void loadDataPack(ResourceManager resources) {
        if (!config.includeDataPack || resources == null) {
            return;
        }
        Map<String, List<String>> extra = new LinkedHashMap<>();
        try {
            Map<ResourceLocation, Resource> found = resources.listResources(
                    "veinminer", id -> id.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                String path = e.getKey().getPath();
                String file = path.substring(path.lastIndexOf('/') + 1);
                String group = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
                try (InputStream in = e.getValue().open()) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    Object parsed = MiniJson.parse(text);
                    if (parsed instanceof Map<?, ?> map) {
                        List<String> blocks = strings(map.get("blocks"));
                        if (!blocks.isEmpty()) {
                            extra.put(group, blocks);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 数据包读取失败不影响启动
        }
        if (!extra.isEmpty()) {
            this.groups = groups.withOverrides(extra);
        }
    }

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
            String bid = blockId(st);
            if (config.blacklist.contains(bid)) {
                return false;
            }
            if (!config.whitelist.isEmpty() && !config.whitelist.contains(bid)) {
                return false;
            }
            List<String> personal = players.allowBlocks(player.getUUID().toString());
            if (!personal.isEmpty() && !personal.contains(bid)) {
                return false;
            }
            return protection.canBreak(handle, p);
        };
        Predicate<Pos> sameGroup = p -> groups.sameGroup(originBlock, blockId(level.getBlockState(blockPos(p))));
        int limit = maxBlocksFor(id);
        PatternContext ctx = new PatternContext(pos, face, axis, limit, canMine, sameGroup);
        PatternId selected = patterns.get(id);
        String patternKey = selected == null ? config.defaultPattern : selected.key;
        BreakPlan plan = engine.plan(patternKey, ctx);
        if (plan.over600()) {
            player.sendSystemMessage(Component.literal(
                    "VeinCore: 本次连锁 " + plan.size() + " 格（>600），卡顿由你自行承担。"));
        }
        // 先挖玩家点的原方块，再逐格连锁；每挖一格扣一次耐久，工具损毁立即停止。
        // 工具剩余耐久不足以覆盖整个计划时，只挖耐久够的那几格。
        DropMerger merger = config.mergeItemDrops ? new DropMerger() : null;
        breakAndCollect(player, origin, merger);
        int destroyed = 1;
        if (config.hungerPerBlock > 0) {
            player.causeFoodExhaustion(config.hungerPerBlock);
        }
        if (!consumeToolOnce(player)) {
            players.recordChain(id.toString(), destroyed);
            savePlayersThrottled();
            if (merger != null) {
                spawnMergedDrops(player, merger);
            }
            return false;
        }
        for (Pos p : plan.blocks()) {
            if (p.equals(pos)) {
                continue;
            }
            breakAndCollect(player, blockPos(p), merger);
            destroyed++;
            if (config.hungerPerBlock > 0) {
                player.causeFoodExhaustion(config.hungerPerBlock);
            }
            if (!consumeToolOnce(player)) {
                break;
            }
        }
        players.recordChain(id.toString(), destroyed);
        savePlayersThrottled();
        if (merger != null) {
            spawnMergedDrops(player, merger);
        }
        return false;
    }

    /** 破坏一格：合并模式先收集掉落物再破坏，否则按原版掉落。 */
    private void breakAndCollect(ServerPlayer player, BlockPos bp, DropMerger merger) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(bp);
        if (state.isAir()) {
            return;
        }
        if (merger == null) {
            platform.requestBreak(new PlayerHandle(player.getUUID().toString()), pos(bp));
            return;
        }
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(bp))
                .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(bp));
        List<ItemStack> drops = state.getDrops(params);
        level.destroyBlock(bp, false);
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            merger.add(pos(bp), BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
        }
    }

    /** 把合并后的掉落物按方块位置弹出。 */
    private void spawnMergedDrops(ServerPlayer player, DropMerger merger) {
        ServerLevel level = player.serverLevel();
        for (DropMerger.Entry entry : merger.entries()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.item());
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), entry.count());
            ItemEntity entity = new ItemEntity(level,
                    entry.pos().x() + 0.5, entry.pos().y() + 0.5, entry.pos().z() + 0.5, stack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }

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

    private boolean authorized(ServerPlayer player) {
        return isOwner(player) || access.isAllowed(player.getUUID().toString());
    }

    private static BlockPos blockPos(Pos p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }

    private static Pos pos(BlockPos p) {
        return new Pos(p.getX(), p.getY(), p.getZ());
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

    private static List<String> strings(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof String s) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}
