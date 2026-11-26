package com.veincore.core.pattern;

import com.veincore.core.Face;
import com.veincore.core.Pos;
import com.veincore.core.ViewAxis;

import java.util.function.Predicate;

/**
 * 模式上下文（不可变）。平台层把 Minecraft 类型转换为 Pos/谓词后传入，common 不认识 MC 类。
 *
 * @param origin    触发方块坐标（首块）
 * @param face      触发面
 * @param viewAxis  玩家视线主轴（square_3x3 用）
 * @param maxBlocks 本次允许的最大方块数
 * @param canMine   单格是否可挖（方块筛选、保护探测、工具判定等）
 * @param sameGroup 两格是否属于同一方块组（vein 用）
 */
public record PatternContext(
        Pos origin,
        Face face,
        ViewAxis viewAxis,
        int maxBlocks,
        Predicate<Pos> canMine,
        Predicate<Pos> sameGroup
) {
}
