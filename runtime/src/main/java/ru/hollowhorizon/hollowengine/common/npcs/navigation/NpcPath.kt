package ru.hollowhorizon.hollowengine.common.npcs.navigation

import net.minecraft.world.entity.Mob
import net.minecraft.world.level.CollisionGetter
import net.minecraft.world.level.pathfinder.Node
import net.minecraft.world.level.pathfinder.Path
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator
import kotlin.math.abs
import kotlin.math.max

/**
 * A node a path steps onto by jumping over a gap. Vanilla's [Path] cannot be extended, so the flag rides on the
 * node, which survives a copy of the path.
 */
internal class JumpNode(source: Node) : Node(source.x, source.y, source.z) {
    init {
        type = source.type
        costMalus = source.costMalus
        walkedDistance = source.walkedDistance
        g = source.g
        h = source.h
        f = source.f
        closed = source.closed
        cameFrom = source.cameFrom
    }
}

/** Whether the step onto node [index] of this path is a jump over a gap. */
internal fun Path.isJumpTo(index: Int): Boolean = index in 0 until nodeCount && getNode(index) is JumpNode

/** Paths whose steps may be jumps over gaps. */
object NpcPath {
    /**
     * [path] as found by the search, its nodes pulled straight in [level] when [straighten] is on. A straight
     * line never cuts across what [avoid] makes the NPC go around; the steps that jump are [JumpNode]s.
     */
    internal fun of(path: Path, level: CollisionGetter, mob: Mob, straighten: Boolean, avoid: AvoidRules?): Path {
        val nodes = List(path.nodeCount, path::getNode)
        val jumps = BooleanArray(nodes.size) { it > 0 && isGapJump(nodes[it - 1], nodes[it]) }
        val kept = if (straighten) {
            PathStraightening.keep(
                nodes,
                jumps,
                canSkip = { it.type in SKIPPABLE },
                canWalk = { from, to -> canWalkStraight(level, mob, from, to, avoid) },
            )
        } else {
            nodes.indices.toList()
        }
        return Path(
            kept.mapTo(ArrayList(kept.size)) { if (jumps[it]) nodes[it] as? JumpNode ?: JumpNode(nodes[it]) else nodes[it] },
            path.target,
            path.canReach(),
        )
    }

    private fun canWalkStraight(level: CollisionGetter, mob: Mob, from: Node, to: Node, avoid: AvoidRules?): Boolean {
        val start = NpcNavigationGeometry.nodeCenter(
            mob,
            from.x,
            WalkNodeEvaluator.getFloorLevel(level, from.asBlockPos()),
            from.z
        )
        val end = NpcNavigationGeometry.nodeCenter(
            mob,
            to.x,
            WalkNodeEvaluator.getFloorLevel(level, to.asBlockPos()),
            to.z
        )
        if (start.distanceToSqr(end) > MAX_STRAIGHT * MAX_STRAIGHT) return false
        return NpcNavigationGeometry.canWalkDirectly(level, mob, start, end) && avoid?.crosses(start, end) != true
    }

    /** The ground a straight line may cross without the NPC having to stop on it, as doors and hazards make it. */
    private val SKIPPABLE = setOf(PathType.WALKABLE, PathType.WATER_BORDER, PathType.OPEN)

    /** The longest straight line a path is pulled into, in blocks. */
    private const val MAX_STRAIGHT = 24.0
}

/** Whether the step from [from] to [to] skips over at least one block: no walking step does. */
internal fun isGapJump(from: Node, to: Node): Boolean = max(abs(to.x - from.x), abs(to.z - from.z)) > 1

/** String pulling: the nodes of a path the NPC can go between in straight lines. */
internal object PathStraightening {
    /**
     * The indices of [nodes] to keep. From each kept node the line runs on as long as [canWalk] goes from it
     * to the next node and every node it passes is one [canSkip] lets go; the ends of jumps, flagged in
     * [jumps] by the index of the node they land on, are always kept.
     */
    fun <T> keep(nodes: List<T>, jumps: BooleanArray, canSkip: (T) -> Boolean, canWalk: (T, T) -> Boolean): List<Int> {
        if (nodes.size <= 2) return nodes.indices.toList()
        val kept = arrayListOf(0)
        val last = nodes.lastIndex
        var anchor = 0
        while (anchor < last) {
            var end = anchor + 1
            if (!jumps[end]) {
                while (end < last && !jumps[end + 1] && canSkip(nodes[end]) && canWalk(
                        nodes[anchor],
                        nodes[end + 1]
                    )
                ) end++
            }
            kept += end
            anchor = end
        }
        return kept
    }
}
