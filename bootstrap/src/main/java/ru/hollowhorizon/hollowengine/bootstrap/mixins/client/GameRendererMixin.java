package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void onRenderItemInHand(CameraRenderState cameraState, float partialTick, Matrix4fc modelViewMatrix, CallbackInfo ci) {
        var camera = ((GameRenderer) (Object) this).mainCamera();
        if (BootstrapRuntimeManager.bridge().onRenderItemInHand(camera, partialTick, new Matrix4f(modelViewMatrix))) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;tryTakeScreenshotIfNeeded()V"))
    private void onLevelFrameRendered(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onLevelFrameRendered(Minecraft.getInstance());
    }
}
