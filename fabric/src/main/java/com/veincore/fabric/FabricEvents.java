package com.veincore.fabric;

import com.veincore.core.Pos;
import com.veincore.platform.IEventRegistrar;
import com.veincore.platform.PlayerHandle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Fabric 事件映射（M1）：把 Fabric API 事件翻译成 common 的回调。 */
public final class FabricEvents implements IEventRegistrar {

    @Override
    public void onServerStart(Runnable callback) {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> callback.run());
    }

    @Override
    public void onServerTick(Runnable callback) {
        ServerTickEvents.END_SERVER_TICK.register(server -> callback.run());
    }

    @Override
    public void onPlayerJoin(Consumer<PlayerHandle> callback) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                callback.accept(new PlayerHandle(handler.getPlayer().getUUID().toString())));
    }

    @Override
    public void onPlayerDisconnect(Consumer<PlayerHandle> callback) {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                callback.accept(new PlayerHandle(handler.getPlayer().getUUID().toString())));
    }

    @Override
    public void onPlayerBreakBlock(BiFunction<PlayerHandle, Pos, Boolean> callback) {
        // BEFORE：方块尚未被破坏，可取消原破坏、由连锁统一接管
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel)) {
                return true;
            }
            return callback.apply(new PlayerHandle(player.getUUID().toString()),
                    new Pos(pos.getX(), pos.getY(), pos.getZ()));
        });
    }

    @Override
    public void onPlayerBreakBlockCancel(Consumer<PlayerHandle> callback) {
        // Fabric 没有"停止挖掘"事件；M1 不使用
    }
}
