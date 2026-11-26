package com.veincore.fabric;

import com.veincore.fabric.net.KeyStatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric 入口（M1）：服务端连锁链路 + 热键状态接收。 */
public final class VeinCoreFabric implements ModInitializer {

    public static final String MOD_ID = "veincore";

    private static VeinCoreService service;

    @Override
    public void onInitialize() {
        // 热键状态通道（C2S）
        PayloadTypeRegistry.playC2S().register(KeyStatePayload.TYPE, KeyStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KeyStatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (service != null) {
                        service.setPressed(context.player().getUUID(), payload.pressed());
                    }
                }));

        // 服务端生命周期
        ServerLifecycleEvents.SERVER_STARTING.register(server -> service = new VeinCoreService(new FabricPlatform(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> service = null);

        // 事件映射
        FabricEvents events = new FabricEvents();
        events.onPlayerBreakBlock((handle, pos) -> service == null || service.handleBreak(handle, pos));
        events.onPlayerDisconnect(handle -> {
            if (service != null) {
                service.forget(handle);
            }
        });
    }
}
