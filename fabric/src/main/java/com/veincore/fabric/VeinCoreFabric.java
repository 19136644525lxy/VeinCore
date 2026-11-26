package com.veincore.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.veincore.fabric.net.KeyStatePayload;
import com.veincore.fabric.net.PatternSelectPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Fabric 入口（M2）：服务端连锁链路 + 热键/模式选择接收 + /veinminer 命令。 */
public final class VeinCoreFabric implements ModInitializer {

    public static final String MOD_ID = "veincore";

    private static VeinCoreService service;

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
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> service = null);

        // 事件映射
        FabricEvents events = new FabricEvents();
        events.onPlayerBreakBlock((handle, pos) -> service == null || service.handleBreak(handle, pos));
        events.onPlayerDisconnect(handle -> {
            if (service != null) {
                service.forget(handle);
            }
        });

        // /veinminer 命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("veinminer")
                .then(Commands.literal("allow")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> allow(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("deny")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> deny(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource()))));
    }

    private static int allow(CommandSourceStack source, ServerPlayer target) {
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可授权"));
            return 0;
        }
        if (service.isOwner(target)) {
            source.sendFailure(Component.literal("该玩家已是 owner，无需授权"));
            return 0;
        }
        if (!service.access().allow(target.getUUID().toString())) {
            source.sendFailure(Component.literal("该玩家已在授权列表中"));
            return 0;
        }
        service.saveAccess();
        source.sendSuccess(() -> Component.literal("已授权 " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int deny(CommandSourceStack source, ServerPlayer target) {
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可取消授权"));
            return 0;
        }
        if (!service.access().deny(target.getUUID().toString())) {
            source.sendFailure(Component.literal("该玩家不在授权列表中（owner 不可取消）"));
            return 0;
        }
        service.saveAccess();
        source.sendSuccess(() -> Component.literal("已取消授权 " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        if (!isOperator(source)) {
            source.sendFailure(Component.literal("仅房主/服主/控制台可查看全部列表；用 /veinminer status 查看自己"));
            return 0;
        }
        String owners = String.join(", ", service.access().owners());
        String allowed = String.join(", ", service.access().allowed());
        source.sendSuccess(() -> Component.literal("owner: " + (owners.isEmpty() ? "（无）" : owners)), false);
        source.sendSuccess(() -> Component.literal("allowed: " + (allowed.isEmpty() ? "（无）" : allowed)), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer caller;
        try {
            caller = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendSuccess(() -> Component.literal("控制台：全权可用"), false);
            return 1;
        }
        if (service == null) {
            source.sendFailure(Component.literal("VeinCore 尚未加载"));
            return 0;
        }
        boolean ok = service.isOwner(caller) || service.isAllowed(caller.getUUID());
        source.sendSuccess(() -> Component.literal(ok ? "VeinCore: 已授权，可用连锁" : "VeinCore: 未授权"), false);
        return 1;
    }

    private static boolean isOperator(CommandSourceStack source) {
        try {
            ServerPlayer caller = source.getPlayerOrException();
            return service != null && service.isOwner(caller);
        } catch (CommandSyntaxException e) {
            return true; // 控制台
        }
    }
}
