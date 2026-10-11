package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Hud.class)
public class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void hollowengine$hideScreen(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (BootstrapRuntimeManager.bridge().shouldHideGui(Minecraft.getInstance().gui.screen())) ci.cancel();
    }
}
