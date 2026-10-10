package ru.hollowhorizon.hollowengine.common.entities.objects

import net.minecraft.core.UUIDUtil
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.ExtraCodecs
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.InterpolationHandler
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.EntityInLevelCallback
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Quaternionfc
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3fc
import ru.hollowhorizon.hollowengine.common.attachments.components.bodyComponent
import ru.hollowhorizon.hollowengine.common.colliders.EntityColliders
import ru.hollowhorizon.hollowengine.common.registry.ModEntities
import java.util.UUID

/**
 * A thing placed in the world: a decoration, a lamp, a lever. It has no health, AI, gravity or physics and
 * never despawns; what it looks like and does comes from its components and node scripts.
 */
class WorldObjectEntity(type: EntityType<WorldObjectEntity>, level: Level) : Entity(type, level) {
    constructor(level: Level) : this(ModEntities.OBJECT, level)

    /** The local pose as it is drawn: on the client it eases toward what the server sent. */
    private var shown = ObjectPose()
    private var shownBefore = ObjectPose()
    private var shownSteps = 0

    /**
     * Eases a root toward the position the server sent. A child follows its parent and the client's own editor
     * has the say while it predicts, so updates meant for neither are dropped.
     */
    private val positionEasing = object : InterpolationHandler(this, LERP_STEPS) {
        override fun interpolateTo(position: Vec3, yRot: Float, xRot: Float) {
            if (parentId != null || tickCount < predictedUntil) return
            super.interpolateTo(position, yRot, xRot)
        }
    }

    /** Set while a change must show at once rather than ease in, like one the editor predicts. */
    private var snapping = false

    /**
     * Until this tick the client shows the pose its own editor set and lets the server's answers to it go by:
     * they are older than what is shown, and easing toward them would pull the object back mid-drag.
     */
    private var predictedUntil = -1

    /** The tick the parent last changed in: the new local pose that comes with it shows at once, not eased from the old one. */
    private var parentChangedTick = -1

    private var shownName: String? = null

    init {
        noPhysics = true
    }

    /** The parent's id as the synced text last read; the text is parsed only when it changes. */
    private var parentText = ""
    private var parentUuid: UUID? = null

    val parentId: UUID?
        get() {
            val text = entityData.get(PARENT)
            if (text != parentText) {
                parentText = text
                parentUuid = if (text.isEmpty()) null else runCatching { UUID.fromString(text) }.getOrNull()
            }
            return parentUuid
        }

    /** The parent, when it is loaded on this side. */
    val parent: WorldObjectEntity?
        get() = parentId?.let { WorldObjects.find(level(), it) }

    /** The pose relative to the parent, or to the world without one. */
    val localPose: ObjectPose
        get() = ObjectPose(
            position = if (parentId == null) Vector3d(x, y, z) else Vector3d(entityData.get(OFFSET)),
            rotation = Quaternionf(entityData.get(ROTATION)),
            scale = Vector3f(entityData.get(SCALE)),
        )

    /** The pose in the world, [partialTick] of the way into the current tick. */
    fun pose(partialTick: Float): ObjectPose = pose(partialTick, 0)

    private fun pose(partialTick: Float, depth: Int): ObjectPose {
        val local = if (partialTick >= 1f) shown else shown.interpolated(shownBefore, partialTick)
        if (parentId == null) return ObjectPose(positionAt(partialTick), local.rotation, local.scale)
        val parent = parent?.takeIf { depth < MAX_DEPTH } ?: return ObjectPose(
            positionAt(partialTick),
            Quaternionf(entityData.get(WORLD_ROTATION)),
            Vector3f(entityData.get(WORLD_SCALE)),
        )
        return parent.pose(partialTick, depth + 1).compose(local)
    }

    /** The entity's own position: a root's, or where a child was left while its parent is not loaded. */
    private fun positionAt(partialTick: Float) = Vector3d(
        Mth.lerp(partialTick.toDouble(), xOld, x),
        Mth.lerp(partialTick.toDouble(), yOld, y),
        Mth.lerp(partialTick.toDouble(), zOld, z),
    )

    /** Places this object so that it ends up at [world], keeping its parent. */
    fun setWorldPose(world: ObjectPose, snap: Boolean = false) {
        if (parentId == null) {
            setLocalPose(world, snap)
            return
        }
        val parent = parent ?: return
        setLocalPose(parent.pose(1f).relativize(world), snap)
    }

    /** Places this object relative to its parent, or in the world without one. */
    fun setLocalPose(local: ObjectPose, snap: Boolean = false) {
        snapping = snap
        if (parentId == null) {
            setPos(local.position.x, local.position.y, local.position.z)
            positionEasing.cancel()
        } else {
            entityData.set(OFFSET, Vector3f().set(local.position))
        }
        entityData.set(ROTATION, Quaternionf(local.rotation).normalize())
        entityData.set(SCALE, Vector3f(local.scale))
        snapping = false
        if (snap) {
            shownBefore = shown
            setOldPosAndRot()
            if (level().isClientSide) predictedUntil = tickCount + PREDICTION_TICKS
        }
        updateWorldCache()
    }

    /**
     * Moves this object under [newParent], or out to the world, without moving it. False when that would
     * put it under itself.
     */
    fun setParent(newParent: WorldObjectEntity?): Boolean {
        if (newParent != null && (newParent === this || newParent.isDescendantOf(this))) return false
        val world = pose(1f)
        entityData.set(PARENT, newParent?.uuid?.toString().orEmpty())
        setWorldPose(world, snap = true)
        return true
    }

    /**
     * Hangs this object under [newParent] keeping its local pose as it is: for a copy of a child, whose
     * pose was taken relative to a parent standing exactly where [newParent] stands.
     */
    internal fun attachKeepingLocalPose(newParent: WorldObjectEntity) {
        entityData.set(PARENT, newParent.uuid.toString())
    }

    /** Whether [ancestor] is above this object, however far up. */
    fun isDescendantOf(ancestor: WorldObjectEntity): Boolean {
        var current = parent
        var depth = 0
        while (current != null && depth++ < MAX_DEPTH) {
            if (current === ancestor) return true
            current = current.parent
        }
        return false
    }

    /** The loaded objects directly under this one. */
    fun children(): List<WorldObjectEntity> = WorldObjects.all(level()).filter { it.parentId == uuid }

    /**
     * This object and every loaded one under it, parents before children. Parents are checked for loops only
     * among loaded objects, so one may still close through an object that was not loaded; it is listed once.
     */
    fun subtree(): List<WorldObjectEntity> {
        val seen = LinkedHashSet<WorldObjectEntity>()
        val queue = ArrayDeque(listOf(this))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (seen.add(next)) queue.addAll(next.children())
        }
        return seen.toList()
    }

    override fun tick() {
        shownBefore = shown
        if (level().isClientSide) ease()
        drift()
        followParent()
        if (parent != null) updateWorldCache()
        firstTick = false
    }

    /**
     * What is left of a push: a root object whose body lets others move it slides along the ground with the
     * world in the way, and slows to a stop. The server moves it; both sides let the speed die out, so the
     * animator, which reads it, stops walking when the object does.
     */
    private fun drift() {
        val motion = deltaMovement
        if (motion == Vec3.ZERO) return
        if (motion.horizontalDistanceSqr() < MIN_DRIFT * MIN_DRIFT || parentId != null) {
            deltaMovement = Vec3.ZERO
            return
        }
        if (!level().isClientSide) {
            noPhysics = false
            move(MoverType.SELF, Vec3(motion.x, 0.0, motion.z))
            noPhysics = true
        }
        deltaMovement = Vec3(motion.x * DRIFT_FRICTION, 0.0, motion.z * DRIFT_FRICTION)
    }

    /** Moves the drawn pose a step toward the one the server sent, the way living entities ease. */
    private fun ease() {
        if (shownSteps > 0) {
            shown = localData().interpolated(shown, 1f / shownSteps)
            shownSteps--
        }
        if (parentId == null && positionEasing.hasActiveInterpolation()) positionEasing.interpolate()
    }

    private fun followParent() {
        val parent = parent ?: return
        val world = parent.pose(1f).compose(shown)
        if (world.position.distanceSquared(x, y, z) > MOVE_EPSILON) {
            setPos(world.position.x, world.position.y, world.position.z)
        }
    }

    /** Keeps the world rotation and scale, which a child shows while its parent is not loaded. */
    private fun updateWorldCache() {
        if (level().isClientSide || parentId != null && parent == null) return
        val world = pose(1f)
        entityData.set(WORLD_ROTATION, world.rotation)
        entityData.set(WORLD_SCALE, world.scale)
    }

    /** The local pose as the synced data has it, offset included. */
    private fun localData() = ObjectPose(
        position = Vector3d(entityData.get(OFFSET)),
        rotation = Quaternionf(entityData.get(ROTATION)),
        scale = Vector3f(entityData.get(SCALE)),
    )

    override fun getInterpolation(): InterpolationHandler = positionEasing

    override fun onSyncedDataUpdated(accessor: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(accessor)
        when (accessor) {
            OFFSET, ROTATION, SCALE -> when {
                !level().isClientSide || snapping || firstTick || tickCount == parentChangedTick -> {
                    shown = localData()
                    shownBefore = shown
                }

                tickCount < predictedUntil -> Unit
                else -> shownSteps = LERP_STEPS
            }

            PARENT -> {
                parentChangedTick = tickCount
                shown = localData()
                shownBefore = shown
                WorldObjects.changed(level())
            }
        }
        val name = customName?.string
        if (name != shownName) {
            shownName = name
            WorldObjects.changed(level())
        }
    }

    /** The components changed, and with them what the scene window shows for this object. */
    fun onComponentsChanged() {
        WorldObjects.changed(level())
    }

    override fun setLevelCallback(callback: EntityInLevelCallback) {
        super.setLevelCallback(callback)
        if (callback === EntityInLevelCallback.NULL) WorldObjects.remove(this) else WorldObjects.add(this)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(PARENT, "")
        builder.define(OFFSET, Vector3f())
        builder.define(ROTATION, Quaternionf())
        builder.define(SCALE, Vector3f(1f))
        builder.define(WORLD_ROTATION, Quaternionf())
        builder.define(WORLD_SCALE, Vector3f(1f))
    }

    /** Met by its colliders, or by its box when the body gives it a size; otherwise the crosshair passes through. */
    override fun isPickable(): Boolean = EntityColliders.hasTargets(this) || bodyComponent?.hasSize == true

    override fun shouldRenderAtSqrDistance(distance: Double): Boolean {
        val reach = RENDER_DISTANCE * getViewScale()
        return distance < reach * reach
    }

    override fun ignoreExplosion(explosion: Explosion): Boolean = true

    override fun isIgnoringBlockTriggers(): Boolean = true

    override fun canTeleport(from: Level, to: Level): Boolean = false

    /** Nothing hurts a placed object; it is removed by the editor, not by damage. */
    override fun hurtServer(level: ServerLevel, source: DamageSource, damage: Float): Boolean = false

    override fun readAdditionalSaveData(input: ValueInput) {
        val data = input.childOrEmpty(SAVE_KEY)
        entityData.set(PARENT, data.read("Parent", UUIDUtil.CODEC).map(UUID::toString).orElse(""))
        data.read("Offset", ExtraCodecs.VECTOR3F).ifPresent { entityData.set(OFFSET, Vector3f(it)) }
        data.read("Rotation", ExtraCodecs.QUATERNIONF).ifPresent { entityData.set(ROTATION, Quaternionf(it).normalize()) }
        data.read("Scale", ExtraCodecs.VECTOR3F).ifPresent { entityData.set(SCALE, Vector3f(it)) }
        data.read("WorldRotation", ExtraCodecs.QUATERNIONF).ifPresent { entityData.set(WORLD_ROTATION, Quaternionf(it).normalize()) }
        data.read("WorldScale", ExtraCodecs.VECTOR3F).ifPresent { entityData.set(WORLD_SCALE, Vector3f(it)) }
        shown = localData()
        shownBefore = shown
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        val data = output.child(SAVE_KEY)
        parentId?.let { data.store("Parent", UUIDUtil.CODEC, it) }
        data.store("Offset", ExtraCodecs.VECTOR3F, entityData.get(OFFSET))
        data.store("Rotation", ExtraCodecs.QUATERNIONF, entityData.get(ROTATION))
        data.store("Scale", ExtraCodecs.VECTOR3F, entityData.get(SCALE))
        data.store("WorldRotation", ExtraCodecs.QUATERNIONF, entityData.get(WORLD_ROTATION))
        data.store("WorldScale", ExtraCodecs.VECTOR3F, entityData.get(WORLD_SCALE))
        if (!level().isClientSide) WorldObjectFavorites.refresh(this)
    }

    override fun remove(reason: RemovalReason) {
        super.remove(reason)
        if (!level().isClientSide && reason.shouldDestroy()) WorldObjectFavorites.forget(this)
    }

    companion object {
        /** The parent's UUID as text, empty without one: 26.x has no UUID serializer for synced data. */
        private val PARENT: EntityDataAccessor<String> =
            SynchedEntityData.defineId(WorldObjectEntity::class.java, EntityDataSerializers.STRING)
        private val OFFSET: EntityDataAccessor<Vector3fc> =
            SynchedEntityData.defineId(WorldObjectEntity::class.java, EntityDataSerializers.VECTOR3)
        private val ROTATION: EntityDataAccessor<Quaternionfc> =
            SynchedEntityData.defineId(WorldObjectEntity::class.java, EntityDataSerializers.QUATERNION)
        private val SCALE: EntityDataAccessor<Vector3fc> =
            SynchedEntityData.defineId(WorldObjectEntity::class.java, EntityDataSerializers.VECTOR3)
        private val WORLD_ROTATION: EntityDataAccessor<Quaternionfc> =
            SynchedEntityData.defineId(WorldObjectEntity::class.java, EntityDataSerializers.QUATERNION)
        private val WORLD_SCALE: EntityDataAccessor<Vector3fc> =
            SynchedEntityData.defineId(WorldObjectEntity::class.java, EntityDataSerializers.VECTOR3)

        private const val SAVE_KEY = "HollowObject"

        /** How deep a hierarchy is followed; deeper parents are treated as missing. */
        private const val MAX_DEPTH = 64

        /** Ticks a change from the server takes to ease in on the client. */
        private const val LERP_STEPS = 3

        /** How long the client trusts its own editor over the server: a second covers the answer even on a slow connection. */
        private const val PREDICTION_TICKS = 20

        /** In blocks; far enough for a decoration to stay in view across its tracking range. */
        private const val RENDER_DISTANCE = 160.0

        private const val MOVE_EPSILON = 1.0e-8

        /** Share of a push kept from one tick to the next, about what ground friction leaves a walking mob. */
        private const val DRIFT_FRICTION = 0.6

        /** Blocks per tick below which a sliding object counts as stopped. */
        private const val MIN_DRIFT = 0.003
    }
}
