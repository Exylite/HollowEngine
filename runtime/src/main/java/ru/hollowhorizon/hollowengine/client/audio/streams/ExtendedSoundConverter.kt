package ru.hollowhorizon.hollowengine.client.audio.streams

import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager

/**
 * What the game's sound lister cannot do: sounds are only ever `.ogg` to it, while the engine also
 * plays `.mp3` and `.wav`, named with their extension in the sound id.
 */
object ExtendedSoundConverter {
    private val extra = listOf(".mp3", ".wav")

    private fun names(path: String) = path.endsWith(".ogg") || extra.any(path::endsWith)

    /** The file for an id that names its own extension, null for the ids the game handles itself. */
    fun fileOf(id: Identifier): Identifier? = if (names(id.path)) id.withPath("sounds/" + id.path) else null

    /** The `.mp3` and `.wav` files of every pack, keyed by file location as the game keys the `.ogg` ones. */
    fun listExtra(resourceManager: ResourceManager): Map<Identifier, Resource> =
        resourceManager.listResources("sounds") { location -> extra.any { location.path.endsWith(it) } }
}
