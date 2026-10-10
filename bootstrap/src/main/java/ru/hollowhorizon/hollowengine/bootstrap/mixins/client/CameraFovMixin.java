package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

/** 26.x computes the world and hand FOV inside {@link Camera}; the old GameRenderer#getFov hook maps here. */
@Mixin(Camera.class)
public class CameraFovMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void onCalculateFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(hollowengine$fov(cir.getReturnValueF(), partialTicks, true));
    }

    @Inject(method = "calculateHudFov", at = @At("RETURN"), cancellable = true)
    private void onCalculateHudFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(hollowengine$fov(cir.getReturnValueF(), partialTicks, false));
    }

    private float hollowengine$fov(float fov, float partialTicks, boolean changingFov) {
        var gameRenderer = Minecraft.getInstance().gameRenderer;
        if (gameRenderer == null) return fov;
        return (float) BootstrapRuntimeManager.bridge().onCameraFov(gameRenderer, (Camera) (Object) this, fov, partialTicks, changingFov);
    }
}
