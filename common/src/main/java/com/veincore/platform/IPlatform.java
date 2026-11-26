package com.veincore.platform;

import com.veincore.core.Pos;

import java.nio.file.Path;

/** 平台基础能力（两端各自实现）。 */
public interface IPlatform {

    /** fabric / forge。 */
    String loaderId();

    boolean isClient();

    boolean isServer();

    Path configDir();

    void info(String message);

    void warn(String message);

    /** 玩家字符串 id（UUID 字符串优先）。 */
    String uuidOf(PlayerHandle player);

    /** 当前服务端 tick 计数。 */
    long serverTick();

    /** 请求平台执行一次方块破坏（是否掉落、经验、工具损耗由平台判定）。 */
    void requestBreak(PlayerHandle player, Pos pos);
}
