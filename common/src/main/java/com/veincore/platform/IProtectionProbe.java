package com.veincore.platform;

import com.veincore.core.Pos;

/** 保护/领地探测抽象。 */
public interface IProtectionProbe {

    /** 玩家是否可在此坐标破坏方块（合成/单机无模组默认 true）。 */
    boolean canBreak(PlayerHandle player, Pos pos);
}
