package com.veincore.platform;

import com.veincore.core.Pos;
import com.veincore.core.pattern.PatternId;

import java.util.List;

/** 客户端渲染抽象：轮廓 + HUD 指示器。 */
public interface IClientRenderer {

    void showOutline(PatternId pattern, List<Pos> blocks, boolean over600);

    void clearOutline();

    void updateHud(PatternId pattern, String patternName, int blockCount, int cooldownTicks, String status);

    void clearHud();
}
