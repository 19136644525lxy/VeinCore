package com.veincore.fabric;

import com.veincore.fabric.net.KeyStatePayload;
import com.veincore.fabric.net.PatternSelectPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric 入口（M3）：服务端连锁链路 + 热键/模式选择接收 + /veinminer 命令 + 数据包加载。 */
public final class VeinCoreFabric implements ModInitializer {

    public static final String MOD_ID = "veincore";

    private static VeinCoreService service;

    static VeinCoreService service() {
        return service;
    }

    @Override
    public void onInitialize() {
        // 热键状态与模式选择通道（C2S）
        PayloadTypeRegistry.playC2S().register(KeyStatePayload.TYPE, KeyStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PatternSelectPayload.TYPE, PatternSelectPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KeyStatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (service != null) {
                        service.setPressed(context.player().getUUID(), payload.pressed());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(PatternSelectPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (service != null) {
                        service.setPattern(context.player().getUUID(), payload.pattern());
                    }
                }));

        // 服务端生命周期
        ServerLifecycleEvents.SERVER_STARTING.register(server -> service = new VeinCoreService(new FabricPlatform(server)));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (service != null) {
                // 资源管理器在 STARTED 阶段已就绪：加载数据包方块组
                service.loadDataPack(server.getResourceManager());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (service != null) {
                service.savePlayers();
            }
            service = null;
        });

        // 事件映射
        FabricEvents events = new FabricEvents();
        events.onPlayerBreakBlock((handle, pos) -> service == null || service.handleBreak(handle, pos));
        events.onPlayerDisconnect(handle -> {
            if (service != null) {
                service.forget(handle);
            }
        });

        // /veinminer 命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> FabricCommands.register(dispatcher));
    }
}
