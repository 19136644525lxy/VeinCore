package com.veincore.fabric.mixin;

import com.veincore.fabric.client.VeinCoreFabricClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 鼠标滚轮：Shift + 连锁热键按住时切换模式（FTB Ultimine 同款交互）。 */
@Mixin(MouseHandler.class)
public abstract class MouseScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void veincore$onScroll(long window, double offsetX, double offsetY, CallbackInfo ci) {
        if (VeinCoreFabricClient.handleScroll(offsetY)) {
            ci.cancel();
        }
    }
}
