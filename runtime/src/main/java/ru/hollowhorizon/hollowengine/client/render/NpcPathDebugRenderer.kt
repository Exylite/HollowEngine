package ru.hollowhorizon.hollowengine.client.render

import net.minecraft.util.Util
import net.minecraft.client.Minecraft
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
        paths[packet.entityId] = DebugPath(path, packet.steeringTarget, Util.getMillis())
    }

    @SubscribeEvent
    fun render(event: RenderLevelStageEvent) {
        if (event.stage != RenderStage.AFTER_ENTITIES || paths.isEmpty()) return

        val now = Util.getMillis()
        paths.values.removeIf { now - it.updatedAt > PATH_TIMEOUT_MS }
        if (paths.isEmpty()) return

        val camera = event.camera.position()
        val buffers = DebugLines.batch(ImmediateBufferSource, event.poseStack)
        for ((_, debugPath) in paths) {
            val nodes = debugPath.path.let { path -> (0 until path.nodeCount).map { path.getNode(it) } }
            nodes.zipWithNext().forEachIndexed { index, (from, to) ->
                val hue = (index + 1).toFloat() / nodes.size * 0.33f
                buffers.line(from.center(camera), to.center(camera), ARGB.opaque(Mth.hsvToRgb(hue, 0.9f, 0.9f)))
            }
            renderSteeringTarget(buffers, camera, debugPath.steeringTarget)
        }
        ImmediateBufferSource.endBatch()
    }

    private fun Node.center(camera: Vec3) =
        Vec3f((x + 0.5 - camera.x).toFloat(), (y + 0.5 - camera.y).toFloat(), (z + 0.5 - camera.z).toFloat())

    /** The steering target as a wireframe box, in the colour vanilla's filled one had. */
    private fun renderSteeringTarget(buffers: DebugLines.Batch, camera: Vec3, target: NpcPathDebugPoint) {
        val bounds = AABB.ofSize(
            Vec3(target.x, target.y + MARKER_HEIGHT * 0.5, target.z),
            MARKER_SIZE,
            MARKER_HEIGHT,
            MARKER_SIZE,
        ).move(-camera.x, -camera.y, -camera.z)
        val center = bounds.center
        buffers.box(
            Vec3f(center.x.toFloat(), center.y.toFloat(), center.z.toFloat()),
            Vec3f((bounds.xsize / 2).toFloat(), 0f, 0f),
            Vec3f(0f, (bounds.ysize / 2).toFloat(), 0f),
            Vec3f(0f, 0f, (bounds.zsize / 2).toFloat()),
            ARGB.colorFromFloat(0.8f, 0.0f, 1.0f, 1.0f),
        )
    }

    private data class DebugPath(
        val path: Path,
        val steeringTarget: NpcPathDebugPoint,
        val updatedAt: Long,
    )

    private const val PATH_TIMEOUT_MS = 2_000L
    private const val NODE_RADIUS = 0.3f
    private const val MARKER_SIZE = 0.2
    private const val MARKER_HEIGHT = 0.7
}
