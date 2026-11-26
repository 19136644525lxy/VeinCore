package com.veincore.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.veincore.config.ClientConfig;
import com.veincore.core.Pos;
import com.veincore.core.pattern.PatternId;
import com.veincore.forge.VeinCoreForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 客户端渲染（M4）：世界轮廓线框 + HUD 指示器（GUI 面板 / 纯文本）。 */
@Mod.EventBusSubscriber(modid = VeinCoreForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeRender {

    private static final int COLOR_READY = 0xA3BE8C;
    private static final int COLOR_WARN = 0xBFBF8C;
    private static final int COLOR_MUTED = 0xAAAAAA;

    private ForgeRender() {
    }

    // ---------- HUD 指示器 ----------

    /** 由 GuiRenderMixin 在 Gui#render 末尾调用（Forge 1.20.5+ 无 HUD 事件，走 Mixin）。 */
    public static void renderHud(GuiGraphics graphics) {
        ClientConfig cfg = ForgeClientConfig.get();
        if (!cfg.enabled || !cfg.indicatorEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        String status = ForgePreview.status();
        if ("idle".equals(status) || "no_target".equals(status)) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int x = width / 2 + cfg.offsetX;
        int y = height / 2 + cfg.offsetY;
        String label = displayName(ForgePreview.pattern());
        String statusText = statusText(status);
        int statusColor = statusColor(status);
        int total = ForgePreview.total();

        if ("text".equalsIgnoreCase(cfg.indicatorMode)) {
            String text = label + " · " + statusText;
            if (cfg.showBlockCount && total > 0) {
                text += " · " + total + " 格";
            }
            graphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF, true);
            return;
        }

        int iconSize = Math.max(8, Math.min(24, cfg.iconSize));
        String top = label;
        String bottom = cfg.showBlockCount && total > 0 ? statusText + " · " + total + " 格" : statusText;
        int textWidth = Math.max(mc.font.width(top), mc.font.width(bottom));
        int panelWidth = iconSize + 6 + textWidth + 6;
        int panelHeight = Math.max(iconSize, 20) + 4;
        graphics.fill(x - 2, y - 2, x + panelWidth, y + panelHeight, 0x99000000);
        int[] src = iconSize(ForgePreview.pattern());
        int drawW = iconSize;
        int drawH = Math.max(1, Math.round(iconSize * (float) src[1] / (float) src[0]));
        int drawY = y + 2 + Math.max(0, (panelHeight - 4 - drawH) / 2);
        graphics.blit(icon(ForgePreview.pattern()), x + 2, drawY, 0f, 0f, drawW, drawH, src[0], src[1]);
        int textX = x + iconSize + 6;
        graphics.drawString(mc.font, Component.literal(top), textX, y + 2, 0xFFFFFF, true);
        graphics.drawString(mc.font, Component.literal(bottom), textX, y + 11, statusColor, true);
    }

    // ---------- 世界轮廓 ----------

    @SubscribeEvent
    public static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        ClientConfig cfg = ForgeClientConfig.get();
        if (!cfg.enabled || !cfg.outlineEnabled) {
            return;
        }
        java.util.List<Pos> blocks = ForgePreview.blocks();
        if (blocks.isEmpty()) {
            return;
        }
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getPoseStack());
        Vec3 cam = event.getCamera().getPosition();
        Minecraft mc = Minecraft.getInstance();
        if (mc.renderBuffers() == null) {
            return;
        }
        MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
        int cap = Math.min(Math.max(1, cfg.previewRenderCap), blocks.size());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer vc = source.getBuffer(RenderType.lines());
        for (int i = 0; i < cap; i++) {
            Pos pos = blocks.get(i);
            LevelRenderer.renderLineBox(pose, vc, pos.x(), pos.y(), pos.z(),
                    pos.x() + 1.0, pos.y() + 1.0, pos.z() + 1.0, 0.95f, 0.95f, 0.95f, 1.0f);
        }
        pose.popPose();
        source.endBatch(RenderType.lines());
    }

    // ---------- 文案与图标 ----------

    static String displayName(String key) {
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

    private static String statusText(String status) {
        return switch (status) {
            case "ready" -> "就绪";
            case "over600" -> "超过 600 格";
            case "no_target" -> "无目标";
            default -> "";
        };
    }

    private static int statusColor(String status) {
        return switch (status) {
            case "ready" -> COLOR_READY;
            case "over600" -> COLOR_WARN;
            case "no_target" -> COLOR_MUTED;
            default -> 0xFFFFFF;
        };
    }

    static ResourceLocation icon(String pattern) {
        String key = PatternId.byKey(pattern) == null ? "unknown" : PatternId.byKey(pattern).key;
        return ResourceLocation.fromNamespaceAndPath("veincore", "textures/pattern_icon/" + key + ".png");
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

    private static int[] iconSize(String pattern) {
        PatternId id = PatternId.byKey(pattern);
        return ICON_SIZES.getOrDefault(id == null ? "unknown" : id.key, new int[]{16, 16});
    }
}
