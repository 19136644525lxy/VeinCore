package com.veincore.fabric.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.veincore.config.ClientConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/** 客户端本地命令：/veinminer indicator ...（只影响本机显示，立即保存到客户端配置）。 */
public final class FabricClientCommands {

    private FabricClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                ClientCommandManager.literal("veinminer")
                        .then(ClientCommandManager.literal("indicator")
                                .executes(ctx -> status(ctx.getSource()))
                                .then(ClientCommandManager.literal("mode")
                                        .then(ClientCommandManager.literal("gui")
                                                .executes(ctx -> setMode(ctx.getSource(), "gui")))
                                        .then(ClientCommandManager.literal("text")
                                                .executes(ctx -> setMode(ctx.getSource(), "text"))))
                                .then(ClientCommandManager.literal("on")
                                        .executes(ctx -> setEnabled(ctx.getSource(), true)))
                                .then(ClientCommandManager.literal("off")
                                        .executes(ctx -> setEnabled(ctx.getSource(), false)))
                                .then(ClientCommandManager.literal("outline")
                                        .then(ClientCommandManager.literal("on")
                                                .executes(ctx -> setOutline(ctx.getSource(), true)))
                                        .then(ClientCommandManager.literal("off")
                                                .executes(ctx -> setOutline(ctx.getSource(), false))))
                                .then(ClientCommandManager.literal("offset")
                                        .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                                                .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                                        .executes(ctx -> setOffset(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                IntegerArgumentType.getInteger(ctx, "y")))))))));
    }

    private static int status(FabricClientCommandSource source) {
        ClientConfig cfg = ClientConfigStore.get();
        source.sendFeedback(Component.literal("指示器：" + (cfg.indicatorEnabled ? "开" : "关")
                + " · 模式=" + cfg.indicatorMode + " · 轮廓=" + (cfg.outlineEnabled ? "开" : "关")
                + " · 偏移=" + cfg.offsetX + "," + cfg.offsetY));
        source.sendFeedback(Component.literal("用法：/veinminer indicator mode <gui|text> | on | off | outline on|off | offset <x> <y>"));
        return 1;
    }

    private static int setMode(FabricClientCommandSource source, String mode) {
        ClientConfigStore.get().indicatorMode = mode;
        ClientConfigStore.save();
        source.sendFeedback(Component.literal("指示器模式：" + mode));
        return 1;
    }

    private static int setEnabled(FabricClientCommandSource source, boolean enabled) {
        ClientConfigStore.get().indicatorEnabled = enabled;
        ClientConfigStore.save();
        source.sendFeedback(Component.literal("指示器：" + (enabled ? "开" : "关")));
        return 1;
    }

    private static int setOutline(FabricClientCommandSource source, boolean enabled) {
        ClientConfigStore.get().outlineEnabled = enabled;
        ClientConfigStore.save();
        source.sendFeedback(Component.literal("轮廓预览：" + (enabled ? "开" : "关")));
        return 1;
    }

    private static int setOffset(FabricClientCommandSource source, int x, int y) {
        ClientConfigStore.get().offsetX = x;
        ClientConfigStore.get().offsetY = y;
        ClientConfigStore.save();
        source.sendFeedback(Component.literal("指示器偏移：" + x + "," + y));
        return 1;
    }
}
