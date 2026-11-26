package com.veincore.core;

import com.veincore.core.pattern.PatternId;

import java.util.List;

/** 一次连锁的破坏计划（引擎产出，平台层逐块执行）。 */
public record BreakPlan(PatternId pattern, List<Pos> blocks, boolean over600) {

    public int size() {
        return blocks.size();
    }
}
