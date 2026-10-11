package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "init(II)V", at = @At("HEAD"))
    private void hollowengine$beforeInit(int width, int height, CallbackInfo ci) {
        var screen = (Screen) (Object) this;
        var redirected = BootstrapRuntimeManager.bridge().onScreenOpen(screen);
        if (screen != redirected) {
            Minecraft.getInstance().gui.setScreen(redirected);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void hollowengine$onRemove(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onScreenClose((Screen) (Object) this);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void hollowengine$onRenderPre(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (BootstrapRuntimeManager.bridge().onScreenRenderPre((Screen) (Object) this, guiGraphics, mouseX, mouseY, partialTick)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void hollowengine$onRenderPost(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onScreenRenderPost((Screen) (Object) this, guiGraphics, mouseX, mouseY, partialTick);
    }
}
