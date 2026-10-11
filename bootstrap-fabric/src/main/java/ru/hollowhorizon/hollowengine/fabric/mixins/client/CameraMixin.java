package ru.hollowhorizon.hollowengine.fabric.mixins.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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
        var camera = (Camera) (Object) this;
        var bridge = BootstrapRuntimeManager.bridge();
        var override = bridge.getCameraOverride(partialTick);

        if (override.active()) {
            ((CameraInvoker) camera).hollowcore$setPosition(override.x(), override.y(), override.z());
        }

        var renderer = Minecraft.getInstance().gameRenderer;
        var setup = bridge.onCameraSetup(renderer, camera, override.active() ? override.yaw() : camera.yRot(), override.active() ? override.pitch() : camera.xRot(), override.active() ? override.roll() : 0.0F, partialTick);
        var accessor = (CameraInvoker) camera;
        accessor.hollowcore$rotate(setup.yaw(), setup.pitch());
        if (setup.roll() != 0.0F) {
            var rotation = accessor.hollowcore$getRotation();
            rotation.rotateZ((float) Math.toRadians(setup.roll()));
            accessor.hollowcore$getForwards().set(0.0F, 0.0F, -1.0F).rotate(rotation);
            accessor.hollowcore$getUp().set(0.0F, 1.0F, 0.0F).rotate(rotation);
            accessor.hollowcore$getLeft().set(-1.0F, 0.0F, 0.0F).rotate(rotation);
        }
    }
}
