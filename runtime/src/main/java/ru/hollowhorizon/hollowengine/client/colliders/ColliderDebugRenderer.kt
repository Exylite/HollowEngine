package ru.hollowhorizon.hollowengine.client.colliders

import net.minecraft.gizmos.Gizmos
import net.minecraft.world.entity.Entity
import ru.hollowhorizon.hollowengine.common.colliders.ColliderModes
import ru.hollowhorizon.hollowengine.common.colliders.EntityColliders

/** Draws an entity's colliders where the server places them, between ticks, in the hitbox view (F3+B). */
object ColliderDebugRenderer {
    /**
     * Draws the colliders of [entity] as gizmos, which the hitbox view collects in world space. True when the
     * colliders stand in for the entity's box, which then is not drawn.
     */
    fun renderHitbox(entity: Entity, partialTick: Float): Boolean {
        ClientColliderTickPoses.at(entity, partialTick).forEach { collider ->
            val color = colorOf(collider.spec.modes)
            collider.volume.outline { start, end -> Gizmos.line(start, end, color, LINE_WIDTH) }
        }
        return EntityColliders.hasTargets(entity)
    }

    /** Clickable colliders draw blue, hit-taking ones green, solid ones orange, ones that only push gray. */
    fun colorOf(modes: ColliderModes): Int = when {
        modes.interact -> INTERACT_COLOR
        modes.hit -> HIT_COLOR
        modes.solid -> SOLID_COLOR
        else -> PUSH_COLOR
    }

    private const val LINE_WIDTH = 2f
    private val HIT_COLOR = 0xCC4DFF99.toInt()
    private val INTERACT_COLOR = 0xCC59B8FF.toInt()
    private val PUSH_COLOR = 0xCCE0E0E0.toInt()
    private val SOLID_COLOR = 0xCCFFB347.toInt()
}
