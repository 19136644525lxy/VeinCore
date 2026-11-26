package com.veincore.platform;

import com.veincore.auth.AccessManager;
import com.veincore.config.ServerConfig;
import com.veincore.core.Pos;
import com.veincore.core.pattern.PatternId;

import java.util.List;

/** 服务端 → 客户端网络抽象（S2C 预览 + 权限/配置同步）。 */
public interface INetwork {

    void sendPreview(PlayerHandle player, PatternId pattern, List<Pos> blocks,
                     int total, int cooldownTicks, String status);

    void sendSync(PlayerHandle player, AccessManager access, ServerConfig config);
}
