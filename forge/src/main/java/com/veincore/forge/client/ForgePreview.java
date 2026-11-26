package com.veincore.forge.client;

import com.veincore.config.ServerConfig;
import com.veincore.core.BlockGroupIndex;
import com.veincore.core.BreakPlan;
import com.veincore.core.Face;
import com.veincore.core.MiningEngine;
import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;
import com.veincore.core.pattern.PatternContext;
import com.veincore.core.pattern.PatternId;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Forge 客户端本地预览扫描（M4）。
 *
 * <p>Forge 端不额外开 S2C 通道：客户端用本地方块数据按当前模式扫描，扫多少格/什么形状与 Fabric 端
 * 服务端预览的视觉结果一致；服务端权限/冷却/封顶仍由服务端逻辑约束（未授权实际不会连锁）。</p>
 */
public final class ForgePreview {

    /** 客户端预览最多渲染的方块数。 */
    private static final int RENDER_CAP = 256;
    /** 每次扫描的步长（tick）。 */
    private static final int SCAN_INTERVAL = 5;

    private static String patternKey = PatternId.VEIN.key;
    private static String status = "idle";
    private static int total;
    private static List<Pos> blocks = List.of();
    private static int age;

    private ForgePreview() {
    }

    /** 更新当前显示的模式（滚轮/命令切换时调用）。 */
    public static void setPattern(String key) {
        if (PatternId.byKey(key) != null) {
            patternKey = key;
        }
    }

    public static String pattern() {
        return patternKey;
    }

    public static String status() {
        return status;
    }

    public static int total() {
        return total;
    }

    public static List<Pos> blocks() {
        return blocks;
    }

    /** 客户端 tick：按节奏刷新扫描结果。 */
    public static void tick(boolean pressed) {
        if (++age < SCAN_INTERVAL) {
            return;
        }
        age = 0;
        if (!pressed) {
            if (!"idle".equals(status)) {
                status = "idle";
                total = 0;
                blocks = List.of();
            }
            return;
        }
        scan();
    }

    private static void scan() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            setEmpty("no_target");
            return;
        }
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            setEmpty("no_target");
            return;
        }
        BlockPos origin = blockHit.getBlockPos();
        BlockState originState = mc.level.getBlockState(origin);
        if (originState.isAir()) {
            setEmpty("no_target");
            return;
        }
        Vec3 look = mc.player.getViewVector(1.0f);
        ViewAxis axis = ViewAxis.fromLook(look.x, look.y, look.z);
        Face face = faceFor(look);
        String originBlock = blockId(originState);
        ServerConfig config = new ServerConfig();
        MiningEngine engine = new MiningEngine(config);
        BlockGroupIndex groups = BlockGroupIndex.from(config.blockGroups);
        int limit = Math.min(config.defaultPlayerBlocks, config.maxBlocks);
        Predicate<Pos> canMine = p -> {
            BlockPos bp = blockPos(p);
            BlockState st = mc.level.getBlockState(bp);
            return !st.isAir() && st.getFluidState().isEmpty() && st.getDestroySpeed(mc.level, bp) >= 0;
        };
        Predicate<Pos> sameGroup = p -> groups.sameGroup(originBlock, blockId(mc.level.getBlockState(blockPos(p))));
        PatternContext ctx = new PatternContext(new Pos(origin.getX(), origin.getY(), origin.getZ()),
                face, axis, limit, canMine, sameGroup);
        BreakPlan plan = engine.plan(patternKey, ctx);
        List<Pos> render = new ArrayList<>();
        Vec3 eye = mc.player.position();
        for (Pos p : plan.blocks()) {
            if (render.size() >= RENDER_CAP) {
                break;
            }
            double dx = p.x() + 0.5 - eye.x;
            double dy = p.y() + 0.5 - eye.y;
            double dz = p.z() + 0.5 - eye.z;
            if (dx * dx + dy * dy + dz * dz <= 64.0 * 64.0) {
                render.add(p);
            }
        }
        status = plan.over600() ? "over600" : "ready";
        total = plan.size();
        blocks = List.copyOf(render);
    }

    private static void setEmpty(String st) {
        status = st;
        total = 0;
        blocks = List.of();
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
}
