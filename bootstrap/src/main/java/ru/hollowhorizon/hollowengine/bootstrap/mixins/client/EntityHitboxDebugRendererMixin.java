package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
    // The hitbox view (F3+B) draws an entity's box first. Where colliders stand in for it, they are drawn instead.
    // The result of Gizmos#cuboid is dropped by the caller, so skipping the call is safe.
    @WrapOperation(
        method = "showHitboxes",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/gizmos/Gizmos;cuboid(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/gizmos/GizmoStyle;)Lnet/minecraft/gizmos/GizmoProperties;",
            ordinal = 0
        )
    )
    private GizmoProperties hollowengine$colliderHitbox(AABB box, GizmoStyle style, Operation<GizmoProperties> original,
                                                        @Local(argsOnly = true) Entity entity, @Local(argsOnly = true) float partialTick) {
        if (BootstrapRuntimeManager.bridge().renderColliderHitbox(entity, partialTick)) return null;
        return original.call(box, style);
    }
}
