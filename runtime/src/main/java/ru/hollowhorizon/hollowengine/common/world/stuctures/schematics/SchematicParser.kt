package ru.hollowhorizon.hollowengine.common.world.stuctures.schematics

import ru.hollowhorizon.hollowengine.common.utils.compat.allKeys
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.commands.arguments.blocks.BlockStateParser
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import ru.hollowhorizon.hollowengine.HollowEngine

object SchematicParser {

    fun parse(root: CompoundTag): Schematic {
        val schemTag = root.getCompoundOrEmpty("Schematic")

        val version = schemTag.getIntOr("Version", 0)
        val dataVersion = schemTag.getIntOr("DataVersion", 0)

        val width = schemTag.getShortOr("Width", 0)
        val height = schemTag.getShortOr("Height", 0)
        val length = schemTag.getShortOr("Length", 0)

        val offset = if (schemTag.contains("Offset")) {
            val array = schemTag.getIntArray("Offset").orElse(IntArray(0))
            BlockPos(array[0], array[1], array[2])
        } else {
            BlockPos.ZERO
        }

        val blockData = if (schemTag.contains("Blocks")) {
            val blocksTag = schemTag.getCompoundOrEmpty("Blocks")
            val paletteTag = blocksTag.getCompoundOrEmpty("Palette")

            val paletteMap = mutableMapOf<Int, BlockInput>()
            paletteTag.allKeys.forEach { key ->
                val key = key.replace("short_grass", "grass")
                try {
                    val state = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, key, true)

                    paletteMap[paletteTag.getIntOr(key, 0)] = BlockInput(state.blockState, state.properties.keys, state.nbt)
                } catch (e: Exception) {
                    HollowEngine.LOGGER.error("Error parsing $key", e)
                    paletteMap[paletteTag.getIntOr(key, 0)] = BlockInput(Blocks.BEDROCK.defaultBlockState(), emptySet(), null)
                }
            }

            val rawData = blocksTag.getByteArray("Data").orElse(ByteArray(0))
            val decodedData = VarIntSerializer.readVarIntArray(rawData, width * height * length)

            val blockEntities = mutableListOf<CompoundTag>()
            if (blocksTag.contains("BlockEntities")) {
                val beList = blocksTag.getListOrEmpty("BlockEntities")
                for (i in 0 until beList.size) {
                    blockEntities.add(beList.getCompoundOrEmpty(i))
                }
            }

            BlockData(paletteMap, decodedData, blockEntities)
        } else null

        val biomeData = if (schemTag.contains("Biomes")) {
            val biomesTag = schemTag.getCompoundOrEmpty("Biomes")
            val paletteTag = biomesTag.getCompoundOrEmpty("Palette")

            val paletteMap = mutableMapOf<Int, String>()
            paletteTag.allKeys.forEach { key ->
                paletteMap[paletteTag.getIntOr(key, 0)] = key
            }

            val rawData = biomesTag.getByteArray("Data").orElse(ByteArray(0))
            val decodedData = VarIntSerializer.readVarIntArray(rawData, width * height * length)

            BiomeData(paletteMap, decodedData)
        } else null

        val entities = mutableListOf<EntityData>()
        if (schemTag.contains("Entities")) {
            val entityList = schemTag.getListOrEmpty("Entities")
            for (i in 0 until entityList.size) {
                val entTag = entityList.getCompoundOrEmpty(i)
                val posList = entTag.getListOrEmpty("Pos")
                val pos = Vec3(posList.getDoubleOr(0, 0.0), posList.getDoubleOr(1, 0.0), posList.getDoubleOr(2, 0.0))
                val id = entTag.getStringOr("Id", "")
                val data = if (entTag.contains("Data")) entTag.getCompoundOrEmpty("Data") else null

                entities.add(EntityData(id, pos, data))
            }
        }

        return Schematic(
            version, dataVersion, width, height, length,
            offset, blockData, biomeData, entities
        )
    }

    fun getIndex(x: Int, y: Int, z: Int, width: Short, length: Short): Int {
        return x + z * width + y * width * length
    }
}