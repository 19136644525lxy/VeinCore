package com.veincore.forge.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.veincore.config.ClientConfig;
import com.veincore.forge.VeinCoreForge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 客户端命令（M4/M5）：/veinminer indicator ...。 */
@Mod.EventBusSubscriber(modid = VeinCoreForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeClientCommands {

    private ForgeClientCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("veinminer")
                .then(Commands.literal("indicator")
                        .then(Commands.literal("mode")
                                .then(Commands.literal("gui").executes(c -> setMode("gui", c.getSource())))
                                .then(Commands.literal("text").executes(c -> setMode("text", c.getSource()))))
                        .then(Commands.literal("on").executes(c -> setEnabled(true, c.getSource())))
                        .then(Commands.literal("off").executes(c -> setEnabled(false, c.getSource())))
                        .then(Commands.literal("outline")
                                .then(Commands.literal("on").executes(c -> setOutline(true, c.getSource())))
                                .then(Commands.literal("off").executes(c -> setOutline(false, c.getSource()))))
                        .then(Commands.literal("offset")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .executes(c -> setOffset(
                                                        IntegerArgumentType.getInteger(c, "x"),
                                                        IntegerArgumentType.getInteger(c, "y"),
                                                        c.getSource())))))));
    }

    private static ClientConfig config() {
        return ForgeClientConfig.get();
    }

    private static int setMode(String mode, CommandSourceStack source) {
        config().indicatorMode = mode;
        ForgeClientConfig.save();
        source.sendSuccess(() -> Component.literal("VeinCore 指示器模式: " + mode), false);
        return 1;
    }

    private static int setEnabled(boolean enabled, CommandSourceStack source) {
        config().indicatorEnabled = enabled;
        ForgeClientConfig.save();
        source.sendSuccess(() -> Component.literal("VeinCore 指示器: " + (enabled ? "开" : "关")), false);
        return 1;
    }

    private static int setOutline(boolean enabled, CommandSourceStack source) {
        config().outlineEnabled = enabled;
        ForgeClientConfig.save();
        source.sendSuccess(() -> Component.literal("VeinCore 轮廓: " + (enabled ? "开" : "关")), false);
        return 1;
    }

    private static int setOffset(int x, int y, CommandSourceStack source) {
        config().offsetX = x;
        config().offsetY = y;
        ForgeClientConfig.save();
        source.sendSuccess(() -> Component.literal("VeinCore 指示器偏移: " + x + ", " + y), false);
        return 1;
    }
}
