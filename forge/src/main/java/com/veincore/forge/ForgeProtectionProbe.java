package com.veincore.forge;

import com.veincore.core.Pos;
import com.veincore.platform.IProtectionProbe;
import com.veincore.platform.PlayerHandle;

/** Forge 保护探测：M2 默认放行，M3 接入领地/保护模组。 */
public final class ForgeProtectionProbe implements IProtectionProbe {

    @Override
    public boolean canBreak(PlayerHandle player, Pos pos) {
        return true;
    }
}
