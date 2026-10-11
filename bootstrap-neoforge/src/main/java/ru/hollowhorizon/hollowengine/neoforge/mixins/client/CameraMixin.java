package ru.hollowhorizon.hollowengine.neoforge.mixins.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;
import ru.hollowhorizon.hollowengine.bridge.mixins.client.CameraInvoker;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void onSetup(float partialTick, CallbackInfo ci) {
        var override = BootstrapRuntimeManager.bridge().getCameraOverride(partialTick);

        if (override.active()) {
            ((CameraInvoker) (Object) this).hollowcore$setPosition(override.x(), override.y(), override.z());
        }
    }
}
