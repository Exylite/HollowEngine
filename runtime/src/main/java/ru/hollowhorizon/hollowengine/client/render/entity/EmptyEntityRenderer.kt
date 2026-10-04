package ru.hollowhorizon.hollowengine.client.render.entity

import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity

/** Draws nothing of its own: the engine's model pipeline does the drawing for entities that use it. */
open class EmptyEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<Entity, EntityRenderState>(context) {
    override fun createRenderState() = EntityRenderState()
}
