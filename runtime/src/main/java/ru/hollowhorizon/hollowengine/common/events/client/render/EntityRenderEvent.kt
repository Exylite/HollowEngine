package ru.hollowhorizon.hollowengine.common.events.client.render

import com.google.common.collect.ImmutableMap
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.object.skull.SkullModelBase
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.player.AbstractClientPlayer
import ru.hollowhorizon.hollowengine.client.render.legacy.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.SkullBlock
import ru.hollowhorizon.hollowengine.common.events.Cancellable
import ru.hollowhorizon.hollowengine.common.events.ClientEvent
import ru.hollowhorizon.hollowengine.common.events.factory.EventHandler
import ru.hollowhorizon.hollowengine.common.utils.JavaHacks

class RegisterEntityLayersDefinitions(private val layerDefinitions: Map<ModelLayerLocation, () -> LayerDefinition>) :
    ClientEvent {
    companion object : EventHandler<RegisterEntityLayersDefinitions>()

    fun registerLayerDefinition(location: ModelLayerLocation, layerDefinition: () -> LayerDefinition) {
        (layerDefinitions as MutableMap)[location] = layerDefinition
    }
}

class AddEntityRendererLayers(
    val renderers: Map<EntityType<*>, EntityRenderer<*, *>>,
    val skinMap: Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>>,
    val context: EntityRendererProvider.Context,
) : ClientEvent {
    companion object : EventHandler<AddEntityRendererLayers>()

    val skins = this.skinMap.keys

    /** The player renderer for one arm model: wide or slim. */
    fun getSkin(skin: PlayerModelType): AvatarRenderer<AbstractClientPlayer> = this.skinMap.getValue(skin)

    fun <R : LivingEntityRenderer<*, *, *>> getRenderer(type: EntityType<*>): R =
        JavaHacks.forceCast(this.renderers[type])

    val entityModels get() = this.context.modelSet
}

class CreateEntitySkullModels(
    private val builder: ImmutableMap.Builder<SkullBlock.Type, SkullModelBase>,
    val entityModelSet: EntityModelSet,
) : ClientEvent {
    companion object : EventHandler<CreateEntitySkullModels>()

    fun registerSkullModel(type: SkullBlock.Type, model: SkullModelBase) {
        builder.put(type, model)
    }
}

open class RenderEntityEvent(
    val entity: Entity,
    val entityYaw: Float,
    val partialTicks: Float,
    val poseStack: PoseStack,
    val buffer: MultiBufferSource,
    val packedLight: Int,
) : ClientEvent {

    class Pre(
        entity: Entity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
    ) : RenderEntityEvent(entity, entityYaw, partialTicks, poseStack, buffer, packedLight), Cancellable {
        companion object : EventHandler<Pre>()

        override var isCanceled = false
    }

    class Post(
        entity: Entity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
    ) : RenderEntityEvent(entity, entityYaw, partialTicks, poseStack, buffer, packedLight) {
        companion object : EventHandler<Post>()
    }

}

class RenderPlayerEvent(
    val player: AbstractClientPlayer,
    val entityYaw: Float,
    val partialTicks: Float,
    val poseStack: PoseStack,
    val buffer: MultiBufferSource,
    val packedLight: Int,
) : ClientEvent, Cancellable {
    companion object : EventHandler<RenderPlayerEvent>()

    override var isCanceled = false
}