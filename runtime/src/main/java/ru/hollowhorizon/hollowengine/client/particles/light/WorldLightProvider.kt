package ru.hollowhorizon.hollowengine.client.particles.light

import ru.hollowhorizon.hollowengine.common.utils.math.Vec3f
import net.minecraft.util.LightCoordsUtil
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import ru.hollowhorizon.hollowengine.client.utils.math.floor

class WorldLightProvider(private val world: Level) : LightProvider {
    override fun query(pos: Vec3f): Int {
        val block = with(pos.floor()) { BlockPos(x.toInt(), y.toInt(), z.toInt()) }
        if (!world.isLoaded(block)) return LightCoordsUtil.FULL_BRIGHT

        return LightCoordsUtil.getLightCoords(world, block)
    }
}