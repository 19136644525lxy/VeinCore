package com.veincore.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.veincore.core.pattern.PatternId;
import com.veincore.fabric.net.KeyStatePayload;
import com.veincore.fabric.net.PatternSelectPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Fabric 客户端入口（M2）：连锁热键状态同步 + Shift+热键+滚轮切换模式。 */
public final class VeinCoreFabricClient implements ClientModInitializer {

    private static final PatternId[] PATTERNS = PatternId.values();
    private static KeyMapping triggerKey;
    private static boolean lastPressed;
    private static int patternIndex;

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.veincore.trigger",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "category.veincore"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                lastPressed = false;
                return;
            }
            boolean pressed = triggerKey.isDown();
            if (pressed != lastPressed) {
                ClientPlayNetworking.send(new KeyStatePayload(pressed));
                lastPressed = pressed;
            }
        });
    }

    /**
     * 滚轮切换模式（FTB Ultimine 同款）：热键按住 + Shift + 滚轮。
     *
     * @return true 表示已消费本次滚轮，不再滚动物品栏
     */
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
        ClientPlayNetworking.send(new PatternSelectPayload(pattern.key));
        client.player.displayClientMessage(Component.literal("VeinCore 模式: " + pattern.key), true);
        return true;
    }
}
