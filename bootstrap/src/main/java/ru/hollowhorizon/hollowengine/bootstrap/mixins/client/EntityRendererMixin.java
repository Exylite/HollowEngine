package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow protected abstract @Nullable Component getNameTag(T entity);

    @WrapOperation(
        method = "shouldRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z",
            ordinal = 0
        )
    )
    private boolean extendCullingBounds(
        Frustum frustum,
        AABB vanillaBounds,
        Operation<Boolean> original,
        @Local(argsOnly = true) T entity
    ) {
        if (BootstrapRuntimeManager.bridge().isEntityFrustumCullingDisabled(entity)) return true;

        var bounds = BootstrapRuntimeManager.bridge().extendEntityCullingBounds(entity, vanillaBounds);
        return original.call(frustum, bounds);
    }

    // The nameplate rides above the colliders of an entity that has some, not above its own box.
    @WrapOperation(
        method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityAttachments;getNullable(Lnet/minecraft/world/entity/EntityAttachment;IF)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 nameplateOverColliders(EntityAttachments attachments, EntityAttachment attachment, int index, float yRot,
                                        Operation<Vec3> original, @Local(argsOnly = true) T entity) {
        return BootstrapRuntimeManager.bridge().entityNameplateAttachment(entity, original.call(attachments, attachment, index, yRot));
    }

    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At("TAIL"))
    private void onExtractNameTags(T entity, S state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo ci) {
        boolean vanillaVisible = state.nameTag != null;
        boolean visible = BootstrapRuntimeManager.bridge().onRenderEntityNameplate(entity, vanillaVisible);
        if (visible == vanillaVisible) return;

        if (visible) {
            state.nameTag = getNameTag(entity);
            state.nameTagAttachment = BootstrapRuntimeManager.bridge().entityNameplateAttachment(
                entity, entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks)));
        } else {
            state.nameTag = null;
            state.scoreText = null;
        }
    }
}
