package ru.hollowhorizon.hollowengine.client.ui.ide.asset

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.data.AtlasIds
import ru.hollowhorizon.hollowengine.client.ui.UiLength
import ru.hollowhorizon.hollowengine.client.ui.style.UiImageUv

internal data class AtlasTexturePreview(val atlas: Identifier, val uv: UiImageUv)

internal fun atlasSpriteId(location: Identifier): Identifier? {
    val path = location.path
    if (!path.startsWith("textures/") || !path.endsWith(".png", ignoreCase = true)) return null
    val sprite = path.removePrefix("textures/").dropLast(".png".length)
    if (sprite.isEmpty()) return null
    if (!sprite.startsWith("block/") && !sprite.startsWith("item/")) return null
    return Identifier.tryBuild(location.namespace, sprite)
}

internal fun atlasTexturePreview(location: Identifier): AtlasTexturePreview? {
    val name = atlasSpriteId(location) ?: return null
    val atlas = runCatching {
        Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
    }.getOrNull() ?: return null
    val sprite = atlas.getSprite(name).takeIf { it.contents().name() == name } ?: return null
    val width = sprite.u1 - sprite.u0
    val height = sprite.v1 - sprite.v0
    if (width <= 0f || height <= 0f) return null
    return AtlasTexturePreview(
        atlas = TextureAtlas.LOCATION_BLOCKS,
        uv = UiImageUv(
            UiLength.Percent(sprite.u0),
            UiLength.Percent(sprite.v0),
            UiLength.Percent(width),
            UiLength.Percent(height),
        ),
    )
}
