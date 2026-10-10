package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;
import ru.hollowhorizon.hollowengine.api.extensions.EntityRenderStateExtension;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Shadow private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;
    @Shadow private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> playerRenderers;

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void onResourceManagerReload(ResourceManager resourceManager, CallbackInfo ci, @Local EntityRendererProvider.Context context) {
        // Vanilla stores immutable maps; give the runtime mutable copies so it can register or wrap renderers.
        Map<EntityType<?>, EntityRenderer<?, ?>> mutableRenderers = new java.util.HashMap<>(renderers);
        Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> mutablePlayerRenderers = new java.util.HashMap<>(playerRenderers);
        BootstrapRuntimeManager.bridge().onAddEntityRendererLayers(mutableRenderers, mutablePlayerRenderers, context);
        renderers = mutableRenderers;
        playerRenderers = mutablePlayerRenderers;
    }

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void onExtractEntity(E entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
        ((EntityRenderStateExtension) cir.getReturnValue()).hollowengine$setEntity(entity, partialTicks);
    }

    @WrapOperation(
        method = "submit",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    )
    private <S extends EntityRenderState> void onSubmitEntity(EntityRenderer<?, S> instance, S state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
        var extension = (EntityRenderStateExtension) state;
        Entity entity = extension.hollowengine$entity();
        if (entity == null) {
            original.call(instance, state, poseStack, collector, camera);
            return;
        }

        var bridge = BootstrapRuntimeManager.bridge();
        float partialTick = extension.hollowengine$partialTick();
        float entityYaw = entity.getYRot(partialTick);
        int light = state.lightCoords;
        if (bridge.onRenderEntityPre(entity, entityYaw, partialTick, poseStack, collector, light)) return;
        boolean cancelled = entity instanceof AbstractClientPlayer player
            && bridge.onRenderPlayer(player, entityYaw, partialTick, poseStack, collector, light);
        if (!cancelled) original.call(instance, state, poseStack, collector, camera);
        bridge.onRenderEntityPost(entity, entityYaw, partialTick, poseStack, collector, light);
    }
}
