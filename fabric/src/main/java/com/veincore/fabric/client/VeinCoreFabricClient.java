package com.veincore.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.veincore.fabric.net.KeyStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Fabric 客户端入口（M1）：连锁热键状态同步。 */
public final class VeinCoreFabricClient implements ClientModInitializer {

    private static KeyMapping triggerKey;
    private static boolean lastPressed;

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
}
