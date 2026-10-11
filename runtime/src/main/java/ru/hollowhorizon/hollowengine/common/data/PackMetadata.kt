package ru.hollowhorizon.hollowengine.common.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.minecraft.SharedConstants
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionType

/**
 * The `pack` section the game's own packs carry, for packs the engine builds in memory: they are made
 * for the running game version, so the format range is just that version's format.
 */
object PackMetadata {
    private fun range(type: PackType): JsonArray {
        val format = SharedConstants.getCurrentVersion().packVersion(type)
        return JsonArray().apply {
            add(format.major())
            add(format.minor())
        }
    }

    fun json(description: String, type: PackType = PackType.CLIENT_RESOURCES): JsonObject = JsonObject().apply {
        add("pack", JsonObject().apply {
            addProperty("description", description)
            add("min_format", range(type))
            add("max_format", range(type))
        })
    }

    /** Answers a metadata section request the way a `pack.mcmeta` with [description] would. */
    fun <T : Any> section(type: MetadataSectionType<T>, description: String): T? {
        if (type.name != "pack") return null
        val pack = json(description).getAsJsonObject("pack")
        return type.codec().parse(JsonOps.INSTANCE, pack).result().orElse(null)
    }
}
