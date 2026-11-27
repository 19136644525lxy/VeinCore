package com.veincore.fabric.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.veincore.config.ClientConfig;
import com.veincore.core.Pos;
import com.veincore.core.pattern.PatternId;
import com.veincore.fabric.net.VeinPreviewPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

/** 客户端渲染（M4/M5）：世界轮廓线框 + HUD 指示器（GUI 面板 / 纯文本）。 */
public final class ClientRender {

    private static final int COLOR_READY = 0xA3BE8C;
    private static final int COLOR_WARN = 0xBFBF8C;
    private static final int COLOR_ERROR = 0xBF616A;
    private static final int COLOR_MUTED = 0xAAAAAA;

    private ClientRender() {
    }

    // ---------- HUD 指示器 ----------

    public static void onHudRender(GuiGraphics graphics, DeltaTracker deltaTracker) {
        ClientConfig cfg = ClientConfigStore.get();
        if (!cfg.enabled || !cfg.indicatorEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        VeinPreviewPayload p = ClientState.preview();
        if (p == null || p.pattern().isEmpty() || "idle".equals(p.status())) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int x = width / 2 + cfg.offsetX;
        int y = height / 2 + cfg.offsetY;
        String label = displayName(p.pattern());
        String status = statusText(p);
        int statusColor = statusColor(p.status());

        if ("text".equalsIgnoreCase(cfg.indicatorMode)) {
            String text = label + " · " + status;
            if (cfg.showBlockCount && p.total() > 0) {
                text += " · " + p.total() + " 格";
            }
            graphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, true);
            return;
        }

        int iconSize = Math.max(8, Math.min(24, cfg.iconSize));
        String top = label;
        String bottom = cfg.showBlockCount && p.total() > 0 ? status + " · " + p.total() + " 格" : status;
        int textWidth = Math.max(mc.font.width(top), mc.font.width(bottom));
        int panelWidth = iconSize + 6 + textWidth + 6;
        int panelHeight = Math.max(iconSize, 20) + 4;
        graphics.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, 0x99000000);
        TextureAtlasSprite sp = iconSprite(p.pattern());
        int sw = sp.contents().width();
        int sh = sp.contents().height();
        int drawW = iconSize;
        int drawH = Math.max(1, Math.round(iconSize * (float) sh / (float) sw));
        int drawY = y + 2 + Math.max(0, (panelHeight - 4 - drawH) / 2);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(x + 2, drawY, 0, drawW, drawH, sp);
        int textX = x + iconSize + 6;
        graphics.drawString(mc.font, Component.literal(top), textX, y + 2, 0xFFFFFF, true);
        graphics.drawString(mc.font, Component.literal(bottom), textX, y + 11, statusColor, true);
    }

    // ---------- 世界轮廓 ----------

    public static void onWorldRender(WorldRenderContext context) {
        ClientConfig cfg = ClientConfigStore.get();
        if (!cfg.enabled || !cfg.outlineEnabled) {
            return;
        }
        VeinPreviewPayload p = ClientState.preview();
        if (p == null || p.blocks().isEmpty()) {
            return;
        }
        PoseStack pose = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (pose == null || consumers == null || context.camera() == null) {
            return;
        }
        Vec3 cam = context.camera().getPosition();
        int cap = Math.min(Math.max(1, cfg.previewRenderCap), p.blocks().size());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer vc = consumers.getBuffer(RenderType.lines());
        for (int i = 0; i < cap; i++) {
            Pos pos = p.blocks().get(i);
            LevelRenderer.renderLineBox(pose, vc, pos.x(), pos.y(), pos.z(),
                    pos.x() + 1.0, pos.y() + 1.0, pos.z() + 1.0, 0.95f, 0.95f, 0.95f, 1.0f);
        }
        pose.popPose();
        if (consumers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RenderType.lines());
        }
    }

    // ---------- 文案与图标 ----------

    public static String displayName(String key) {
        PatternId id = PatternId.byKey(key);
        if (id == null) {
            return "未知模式";
        }
        return switch (id) {
            case VEIN -> "矿脉";
            case CHAIN -> "连锁";
            case SQUARE_3X3 -> "3×3 小方块";
            case TUNNEL_1X1 -> "隧道 1×1";
            case TUNNEL_3X3 -> "隧道 3×3";
            case DIAGONAL -> "斜向";
            case PLANE -> "平面";
        };
    }

    private static String statusText(VeinPreviewPayload p) {
        return switch (p.status()) {
            case "ready" -> "就绪";
            case "over600" -> "超过 600 格";
            case "cooldown" -> String.format("冷却 %.1fs", p.cooldownTicks() / 20.0);
            case "unauthorized" -> "未授权";
            case "no_target" -> "无目标";
            default -> "";
        };
    }

    private static int statusColor(String status) {
        return switch (status) {
            case "ready" -> COLOR_READY;
            case "over600", "cooldown" -> COLOR_WARN;
            case "unauthorized" -> COLOR_ERROR;
            case "no_target" -> COLOR_MUTED;
            default -> 0xFFFFFF;
        };
    }

    /** 各 pattern_id 原始 PNG 尺寸（与 NOTICE 映射一致，未改像素）。 */
    private static final java.util.Map<String, int[]> ICON_SIZES = java.util.Map.of(
            "vein", new int[]{17, 15},
            "chain", new int[]{15, 15},
            "square_3x3", new int[]{13, 13},
            "tunnel_1x1", new int[]{13, 13},
            "tunnel_3x3", new int[]{17, 17},
            "diagonal", new int[]{15, 15},
            "plane", new int[]{17, 12},
            "unknown", new int[]{12, 17});

    /**
     * atlas 中的图标 sprite：assets/veincore/textures/pattern_icon/<key>.png，
     * 由 assets/minecraft/atlases/blocks.json 注入方块图集；缺失回退 unknown。
     */
    private static TextureAtlasSprite iconSprite(String pattern) {
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        String key = PatternId.byKey(pattern) == null ? "unknown" : PatternId.byKey(pattern).key;
        TextureAtlasSprite sprite = atlas.getSprite(ResourceLocation.fromNamespaceAndPath("veincore", "pattern_icon/" + key));
        if (MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return atlas.getSprite(ResourceLocation.fromNamespaceAndPath("veincore", "pattern_icon/unknown"));
        }
        return sprite;
    }
}
