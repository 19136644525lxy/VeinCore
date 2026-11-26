package com.veincore.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.veincore.core.pattern.PatternId;
import com.veincore.fabric.net.KeyStatePayload;
import com.veincore.fabric.net.PatternSelectPayload;
import com.veincore.fabric.net.VeinPreviewPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Fabric 客户端入口（M2/M4/M5）：热键同步、滚轮切模式、预览接收、轮廓渲染与 HUD 指示器。 */
public final class VeinCoreFabricClient implements ClientModInitializer {

    private static final PatternId[] PATTERNS = PatternId.values();
    private static KeyMapping triggerKey;
    private static boolean lastPressed;
    private static boolean lastDown;
    private static boolean toggleActive;
    private static int patternIndex;

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.veincore.trigger",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "category.veincore"));

        // 服务端预览 → 客户端状态
        ClientPlayNetworking.registerGlobalReceiver(VeinPreviewPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientState.update(payload)));

        // HUD 指示器与轮廓渲染
        HudRenderCallback.EVENT.register(ClientRender::onHudRender);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientRender::onWorldRender);

        // 客户端本地命令
        FabricClientCommands.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientState.tick();
            if (client.player == null) {
                lastPressed = false;
                lastDown = false;
                toggleActive = false;
                return;
            }
            boolean down = triggerKey.isDown();
            if (ClientConfigStore.get().toggleMode) {
                // 切换式：按一下热键切换“连锁开启/关闭”，而不是按住。
                if (down && !lastDown) {
                    toggleActive = !toggleActive;
                    ClientPlayNetworking.send(new KeyStatePayload(toggleActive));
                }
            } else if (down != lastPressed) {
                ClientPlayNetworking.send(new KeyStatePayload(down));
                lastPressed = down;
            }
            lastDown = down;
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
        client.player.displayClientMessage(Component.literal("VeinCore 模式: " + ClientRender.displayName(pattern.key)), true);
        return true;
    }
}
