package com.veincore.platform;

import com.veincore.core.Pos;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 事件注册抽象：两端把各自事件映射到这些回调。 */
public interface IEventRegistrar {

    void onServerStart(Runnable callback);

    void onServerTick(Runnable callback);

    void onPlayerJoin(Consumer<PlayerHandle> callback);

    void onPlayerDisconnect(Consumer<PlayerHandle> callback);

    /** 玩家左键/破坏方块（触发点）。 */
    void onPlayerBreakBlock(BiConsumer<PlayerHandle, Pos> callback);

    /** 玩家松手动块键（取消预览等）。 */
    void onPlayerBreakBlockCancel(Consumer<PlayerHandle> callback);
}
