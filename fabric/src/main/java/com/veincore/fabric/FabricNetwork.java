package com.veincore.fabric;

import com.veincore.auth.AccessManager;
import com.veincore.config.ServerConfig;
import com.veincore.core.Pos;
import com.veincore.core.pattern.PatternId;
import com.veincore.platform.INetwork;
import com.veincore.platform.PlayerHandle;

import java.util.List;

/** Fabric 网络实现：M1 只占位，预览/同步在 M4 实现。 */
public final class FabricNetwork implements INetwork {

    @Override
    public void sendPreview(PlayerHandle player, PatternId pattern, List<Pos> blocks,
                            int total, int cooldownTicks, String status) {
        // M4：发送预览轮廓
    }

    @Override
    public void sendSync(PlayerHandle player, AccessManager access, ServerConfig config) {
        // M3/M4：同步权限与配置
    }
}
