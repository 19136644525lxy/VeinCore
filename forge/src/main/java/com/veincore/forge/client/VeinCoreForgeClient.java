package com.veincore.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.veincore.core.pattern.PatternId;
import com.veincore.forge.VeinCoreForge;
import com.veincore.forge.net.ForgeNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Forge 客户端入口（M2）：连锁热键状态同步 + Shift+热键+滚轮切换模式。 */
public final class VeinCoreForgeClient {

    private static final PatternId[] PATTERNS = PatternId.values();
    private static KeyMapping triggerKey;
    private static boolean lastPressed;
    private static int patternIndex;

    private VeinCoreForgeClient() {
    }

    @Mod.EventBusSubscriber(modid = VeinCoreForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            triggerKey = new KeyMapping("key.veincore.trigger", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_GRAVE_ACCENT, "category.veincore");
            event.register(triggerKey);
        }
    }

    @Mod.EventBusSubscriber(modid = VeinCoreForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || triggerKey == null) {
                lastPressed = false;
                return;
            }
            boolean pressed = triggerKey.isDown();
            if (pressed != lastPressed) {
                ForgeNetwork.sendKeyState(pressed);
                lastPressed = pressed;
            }
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            if (handleScroll(event.getDeltaY())) {
                event.setCanceled(true);
            }
        }
    }

    /** 滚轮切换模式（FTB Ultimine 同款）：热键按住 + Shift + 滚轮。 */
    public static boolean handleScroll(double offsetY) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null || triggerKey == null) {
            return false;
        }
        if (offsetY == 0 || !triggerKey.isDown() || !Screen.hasShiftDown()) {
            return false;
        }
        patternIndex = Math.floorMod(patternIndex + (offsetY > 0 ? 1 : -1), PATTERNS.length);
        PatternId pattern = PATTERNS[patternIndex];
        ForgeNetwork.sendPattern(pattern.key);
        client.player.displayClientMessage(Component.literal("VeinCore 模式: " + pattern.key), true);
        return true;
    }
}
