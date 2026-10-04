package ru.hollowhorizon.hollowengine.client.models.internal.v2

import ru.hollowhorizon.hollowengine.client.render.legacy.RecordingBufferSource
import net.minecraft.client.renderer.item.ItemStackRenderState
import ru.hollowhorizon.hollowengine.common.utils.math.MutableMat3f
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Quaternionf
import ru.hollowhorizon.hollowengine.client.models.internal.rendering.RenderPipeline
import ru.hollowhorizon.hollowengine.client.utils.math.asMatrix3f
import ru.hollowhorizon.hollowengine.client.utils.math.asMatrix4f

class ItemNode(val entity: () -> LivingEntity?, val slot: EquipmentSlot, parent: Attachment?): Attachment(parent) {
    override fun collectCommands(pipeline: RenderPipeline) {
        super.collectCommands(pipeline)
        pipeline.addBatchedRenderable {
            val entity = entity() ?: return@addBatchedRenderable
            stack.pushPose()

            stack.mulPose(globalMatrix.asMatrix4f())
            stack.last().normal().mul(globalMatrix.getUpperLeft(MutableMat3f()).asMatrix3f())

            stack.mulPose(Quaternionf().rotateX(-90 * Mth.DEG_TO_RAD))

            // vanilla draws items as part of its own submit pass, so this only works while an entity is being submitted
            val collector = (source as? RecordingBufferSource)?.collector
            if (collector != null) {
                val item = entity.getItemBySlot(slot)
                val minecraft = Minecraft.getInstance()
                val renderState = ItemStackRenderState()
                minecraft.itemModelResolver.updateForLiving(
                    renderState,
                    item,
                    when (slot) {
                        EquipmentSlot.MAINHAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                        EquipmentSlot.OFFHAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        EquipmentSlot.HEAD -> ItemDisplayContext.HEAD
                        else -> ItemDisplayContext.FIXED
                    },
                    entity,
                )
                renderState.submit(stack, collector, light, overlay, 0)
            }

            stack.popPose()
        }
    }
}