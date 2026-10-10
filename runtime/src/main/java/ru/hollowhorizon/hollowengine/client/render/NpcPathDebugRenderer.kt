package ru.hollowhorizon.hollowengine.client.render

import net.minecraft.util.Util
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import ru.hollowhorizon.hollowengine.client.render.legacy.ImmediateBufferSource
import ru.hollowhorizon.hollowengine.common.utils.math.Vec3f
import net.minecraft.core.BlockPos
import net.minecraft.world.level.pathfinder.Node
import net.minecraft.world.level.pathfinder.Path
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import ru.hollowhorizon.hollowengine.common.events.ClientOnly
import ru.hollowhorizon.hollowengine.common.events.SubscribeEvent
import ru.hollowhorizon.hollowengine.common.events.client.render.RenderLevelStageEvent
import ru.hollowhorizon.hollowengine.common.events.client.render.RenderStage
import ru.hollowhorizon.hollowengine.common.npcs.navigation.NpcPathDebugPacket
import ru.hollowhorizon.hollowengine.common.npcs.navigation.NpcPathDebugPoint

/** The path an NPC follows, where it heads and looks, and its jumps. The speed share label of 1.21 is not drawn: 26.x has no floating text for the level. */
@ClientOnly
object NpcPathDebugRenderer {
    private val paths = mutableMapOf<Int, DebugPath>()

    fun update(packet: NpcPathDebugPacket) {
        if (packet.nodes.isEmpty()) {
            paths.remove(packet.entityId)
            return
        }

        val nodes = packet.nodes.map { debugNode ->
            Node(debugNode.x, debugNode.y, debugNode.z).apply {
                type = PathType.valueOf(debugNode.type)
                costMalus = debugNode.costMalus
            }
        }
        val path = Path(
            nodes,
            BlockPos(packet.targetX, packet.targetY, packet.targetZ),
            packet.reached,
        ).apply {
            nextNodeIndex = packet.nextNodeIndex.coerceIn(0, nodes.lastIndex)
        }
        val jumps = packet.nodes.indices.filter { packet.nodes[it].jump }
        paths[packet.entityId] = DebugPath(path, jumps, packet.steeringTarget, packet.lookTarget, Util.getMillis())
    }

    @SubscribeEvent
    fun render(event: RenderLevelStageEvent) {
        if (event.stage != RenderStage.AFTER_ENTITIES || paths.isEmpty()) return

        val now = Util.getMillis()
        paths.values.removeIf { now - it.updatedAt > PATH_TIMEOUT_MS }
        if (paths.isEmpty()) return

        val camera = event.camera.position()
        val buffers = DebugLines.batch(ImmediateBufferSource, event.poseStack, DebugLines.WORLD)
        for ((_, debugPath) in paths) {
            val nodes = debugPath.path.let { path -> (0 until path.nodeCount).map { path.getNode(it) } }
            nodes.zipWithNext().forEachIndexed { index, (from, to) ->
                val hue = (index + 1).toFloat() / nodes.size * 0.33f
                buffers.line(from.center(camera), to.center(camera), ARGB.opaque(Mth.hsvToRgb(hue, 0.9f, 0.9f)))
            }
            renderMarker(buffers, camera, debugPath.steeringTarget, MARKER_SIZE, STEERING_COLOR)
            debugPath.lookTarget?.let { renderMarker(buffers, camera, it, LOOK_MARKER_SIZE, LOOK_COLOR) }
            renderJumps(buffers, camera, debugPath)
        }
        ImmediateBufferSource.endBatch()
    }

    private fun Node.center(camera: Vec3) =
        Vec3f((x + 0.5 - camera.x).toFloat(), (y + 0.5 - camera.y).toFloat(), (z + 0.5 - camera.z).toFloat())

    /** A post at [target] as a wireframe box: cyan where the NPC heads, magenta where it looks. */
    private fun renderMarker(buffers: DebugLines.Batch, camera: Vec3, target: NpcPathDebugPoint, size: Double, color: Int) {
        val bounds = AABB.ofSize(
            Vec3(target.x, target.y + MARKER_HEIGHT * 0.5, target.z),
            size,
            MARKER_HEIGHT,
            size,
        ).move(-camera.x, -camera.y, -camera.z)
        val center = bounds.center
        buffers.box(
            Vec3f(center.x.toFloat(), center.y.toFloat(), center.z.toFloat()),
            Vec3f((bounds.xsize / 2).toFloat(), 0f, 0f),
            Vec3f(0f, (bounds.ysize / 2).toFloat(), 0f),
            Vec3f(0f, 0f, (bounds.zsize / 2).toFloat()),
            color,
        )
    }

    private fun renderJumps(buffers: DebugLines.Batch, camera: Vec3, debugPath: DebugPath) {
        for (index in debugPath.jumps) {
            if (index == 0) continue
            val from = debugPath.path.getNode(index - 1)
            val to = debugPath.path.getNode(index)
            var previous = arcPoint(from, to, 0f, camera)
            for (step in 1..ARC_SEGMENTS) {
                val point = arcPoint(from, to, step.toFloat() / ARC_SEGMENTS, camera)
                buffers.line(previous, point, JUMP_COLOR)
                previous = point
            }
        }
    }

    private fun arcPoint(from: Node, to: Node, t: Float, camera: Vec3): Vec3f {
        val height = from.y + (to.y - from.y) * t + ARC_HEIGHT * 4f * t * (1f - t)
        return Vec3f(
            (from.x + 0.5f + (to.x - from.x) * t - camera.x).toFloat(),
            (height - camera.y).toFloat(),
            (from.z + 0.5f + (to.z - from.z) * t - camera.z).toFloat(),
        )
    }

    private data class DebugPath(
        val path: Path,
        val jumps: List<Int>,
        val steeringTarget: NpcPathDebugPoint,
        val lookTarget: NpcPathDebugPoint?,
        val updatedAt: Long,
    )

    private const val PATH_TIMEOUT_MS = 2_000L
    private const val MARKER_SIZE = 0.2
    private const val MARKER_HEIGHT = 0.7
    private const val LOOK_MARKER_SIZE = 0.1
    private const val ARC_SEGMENTS = 12
    private const val ARC_HEIGHT = 1.25f
    private const val JUMP_COLOR = 0xFFFFAA00.toInt()
    private val STEERING_COLOR = ARGB.colorFromFloat(0.8f, 0.0f, 1.0f, 1.0f)
    private val LOOK_COLOR = ARGB.colorFromFloat(0.8f, 1.0f, 0.2f, 1.0f)
}
