package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @WrapOperation(
        method = {"renderPlayerArm", "renderMapHand"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V")
    )
    private void onRenderRightHand(AvatarRenderer<AbstractClientPlayer> renderer, PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original) {
        if (minecraft.player != null && BootstrapRuntimeManager.bridge().onRenderArm(poseStack, collector, light, minecraft.player, HumanoidArm.RIGHT)) return;
        original.call(renderer, poseStack, collector, light, skin, sleeve);
    }

    @WrapOperation(
        method = {"renderPlayerArm", "renderMapHand"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V")
    )
    private void onRenderLeftHand(AvatarRenderer<AbstractClientPlayer> renderer, PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, Operation<Void> original) {
        if (minecraft.player != null && BootstrapRuntimeManager.bridge().onRenderArm(poseStack, collector, light, minecraft.player, HumanoidArm.LEFT)) return;
        original.call(renderer, poseStack, collector, light, skin, sleeve);
    }
}
