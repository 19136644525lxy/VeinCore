package com.veincore.fabric;

import com.veincore.core.Pos;
import com.veincore.fabric.net.KeyStatePayload;
import com.veincore.fabric.net.PatternSelectPayload;
import com.veincore.fabric.net.VeinPreviewPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** Fabric 入口（M3/M4）：服务端连锁链路 + 热键/模式选择接收 + 预览推送 + 命令 + 数据包加载。 */
public final class VeinCoreFabric implements ModInitializer {

    public static final String MOD_ID = "veincore";
    /** 按住热键时预览刷新间隔（tick）。 */
    private static final int PREVIEW_INTERVAL_TICKS = 10;

    private static VeinCoreService service;

    static VeinCoreService service() {
        return service;
    }

    @Override
    public void onInitialize() {
        // 热键状态与模式选择通道（C2S）+ 预览（S2C）
        PayloadTypeRegistry.playC2S().register(KeyStatePayload.TYPE, KeyStatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PatternSelectPayload.TYPE, PatternSelectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VeinPreviewPayload.TYPE, VeinPreviewPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KeyStatePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (service == null) {
                        return;
                    }
                    service.setPressed(context.player().getUUID(), payload.pressed());
                    if (payload.pressed()) {
                        pushPreview(context.player());
                    } else {
                        clearPreview(context.player());
                    }
                }));
        ServerPlayNetworking.registerGlobalReceiver(PatternSelectPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (service == null) {
                        return;
                    }
                    service.setPattern(context.player().getUUID(), payload.pattern());
                    pushPreview(context.player());
                }));

        // 按住热键期间周期性刷新预览（冷却/目标变化）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (service == null || server.getTickCount() % PREVIEW_INTERVAL_TICKS != 0) {
                return;
            }
            for (UUID id : service.pressedIds()) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    pushPreview(player);
                }
            }
        });

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

    static void pushPreview(ServerPlayer player) {
        if (service == null) {
            return;
        }
        PreviewResult result = service.preview(player);
        ServerPlayNetworking.send(player, new VeinPreviewPayload(
                result.pattern(), result.status(), result.total(), result.cooldownTicks(), result.blocks()));
    }

    private static void clearPreview(ServerPlayer player) {
        ServerPlayNetworking.send(player, new VeinPreviewPayload("", "idle", 0, 0, List.<Pos>of()));
    }
}
