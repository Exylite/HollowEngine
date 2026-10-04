package ru.hollowhorizon.hollowengine.client.audio.streams

import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import ru.hollowhorizon.hollowengine.common.utils.rl

object ExtendedSoundConverter : FileToIdConverter("sounds", ".ogg") {
    override fun idToFile(id: Identifier): Identifier {
        if (id.path.let { it.endsWith(".mp3") || it.endsWith(".wav") || it.endsWith(".ogg") }) {
            return id.withPath("sounds/" + id.path)
        }
        return super.idToFile(id)
    }

    override fun fileToId(file: Identifier): Identifier {
        if (file.path.let { it.endsWith(".mp3") || it.endsWith(".wav") || it.endsWith(".ogg") }) {
            return "${file.namespace}:${file.path.substringAfter("sounds/")}".rl
        }
        return super.fileToId(file)
    }

    override fun listMatchingResources(resourceManager: ResourceManager): MutableMap<Identifier, Resource> {
        return resourceManager.listResources(
            "sounds"
        ) { it.path.let { it.endsWith(".ogg") || it.endsWith(".mp3") || it.endsWith(".wav") } }
    }

    override fun listMatchingResourceStacks(resourceManager: ResourceManager): MutableMap<Identifier, MutableList<Resource>> {
        return resourceManager.listResourceStacks(
            "sounds"
        ) { it.path.let { it.endsWith(".ogg") || it.endsWith(".mp3") || it.endsWith(".wav") } }
    }
}