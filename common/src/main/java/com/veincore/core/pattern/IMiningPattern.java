package com.veincore.core.pattern;

import com.veincore.core.Pos;

import java.util.List;

/** 挖掘模式：输出要破坏的方块坐标列表（含 origin，已按 maxBlocks 截断）。 */
public interface IMiningPattern {

    PatternId id();

    /**
     * @param ctx 不可变的模式上下文（原点、面、视线轴、上限、过滤谓词）
     * @return 按执行顺序的方块坐标；首元素约定为 origin
     */
    List<Pos> collect(PatternContext ctx);
}
