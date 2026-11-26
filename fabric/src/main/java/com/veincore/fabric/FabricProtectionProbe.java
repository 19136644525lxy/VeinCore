package com.veincore.fabric;

import com.veincore.core.Pos;
import com.veincore.platform.IProtectionProbe;
import com.veincore.platform.PlayerHandle;

/** 保护探测：M1 默认放行；M2 接入 FTB Chunks 等。 */
public final class FabricProtectionProbe implements IProtectionProbe {

    @Override
    public boolean canBreak(PlayerHandle player, Pos pos) {
        return true;
    }
}
