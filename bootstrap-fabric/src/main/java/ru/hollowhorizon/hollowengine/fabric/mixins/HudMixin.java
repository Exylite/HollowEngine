package ru.hollowhorizon.hollowengine.fabric.mixins;

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
public class HudMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRenderHudPost(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onRenderHudPost(Minecraft.getInstance().getWindow(), graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
