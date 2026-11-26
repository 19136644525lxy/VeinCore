package com.veincore.forge.mixin;

import com.veincore.forge.client.ForgeRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge 1.20.5+ 移除了通用 HUD 渲染事件（RenderGuiEvent 被删除），
 * 因此通过 Mixin 在 {@link Gui#render} 末尾绘制指示器面板。
 */
@Mixin(Gui.class)
public class GuiRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void veincore$renderIndicator(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ForgeRender.renderHud(guiGraphics);
    }
}
