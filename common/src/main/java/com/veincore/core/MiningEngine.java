package com.veincore.core;

import com.veincore.config.ServerConfig;
import com.veincore.core.pattern.ChainPattern;
import com.veincore.core.pattern.DiagonalPattern;
import com.veincore.core.pattern.IMiningPattern;
import com.veincore.core.pattern.PatternContext;
import com.veincore.core.pattern.PatternId;
import com.veincore.core.pattern.PlanePattern;
import com.veincore.core.pattern.Square3x3Pattern;
import com.veincore.core.pattern.Tunnel1x1Pattern;
import com.veincore.core.pattern.Tunnel3x3Pattern;
import com.veincore.core.pattern.VeinPattern;

import java.util.List;
import java.util.Objects;

/**
 * 一次连锁的编排：模式解析 → 扫描 → 上限夹取 → 生成 BreakPlan（M1 核心引擎）。
 *
 * <p>本类不接触任何 Minecraft 类型；平台层负责把世界转换为 Pos 谓词，并把
 * {@link BreakPlan#blocks()} 逐格执行破坏。</p>
 */
public final class MiningEngine {

    private final ServerConfig config;

    public MiningEngine(ServerConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * 生成一次连锁的破坏计划。
     *
     * @param patternKey 玩家当前选择（可能为 null 或未知 key，回退 default_pattern，再回退 vein）
     * @param context    平台层构造的上下文（maxBlocks 会被服务器封顶再夹取）
     */
    public BreakPlan plan(String patternKey, PatternContext context) {
        PatternId id = resolve(patternKey);
        IMiningPattern pattern = patternFor(id);
        int cap = MaxBlocksLimiter.effective(context.maxBlocks(), config.maxBlocks);
        PatternContext bounded = new PatternContext(
                context.origin(), context.face(), context.viewAxis(), cap,
                context.canMine(), context.sameGroup());
        List<Pos> blocks = pattern.collect(bounded);
        return new BreakPlan(id, List.copyOf(blocks), MaxBlocksLimiter.overWarn(blocks.size()));
    }

    /** 模式 key → PatternId；未知回退 default_pattern，再回退 VEIN。 */
    public PatternId resolve(String patternKey) {
        PatternId id = patternKey == null ? null : PatternId.byKey(patternKey);
        if (id != null) {
            return id;
        }
        id = PatternId.byKey(config.defaultPattern);
        return id != null ? id : PatternId.VEIN;
    }

    public static IMiningPattern patternFor(PatternId id) {
        return switch (id) {
            case VEIN -> new VeinPattern();
            case CHAIN -> new ChainPattern();
            case SQUARE_3X3 -> new Square3x3Pattern();
            case TUNNEL_1X1 -> new Tunnel1x1Pattern();
            case TUNNEL_3X3 -> new Tunnel3x3Pattern();
            case DIAGONAL -> new DiagonalPattern();
            case PLANE -> new PlanePattern();
        };
    }
}
