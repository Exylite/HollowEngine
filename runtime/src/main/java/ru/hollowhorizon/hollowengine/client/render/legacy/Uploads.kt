package ru.hollowhorizon.hollowengine.client.render.legacy

import com.mojang.blaze3d.platform.NativeImage
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL14

/** Allocates storage for the bound texture, as `TextureUtil.prepareImage` did. */
fun LegacyGl.prepareImage(textureId: Int, width: Int, height: Int) {
    RenderSystem.bindTexture(textureId)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0)
    GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0f)
    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L)
}

/**
 * What `NativeImage.upload` did: sends a rectangle of the image into the texture bound at the moment.
 * Pixels sit in memory as RGBA bytes, which is what the GL format says.
 */
fun NativeImage.upload(
    level: Int,
    xOffset: Int,
    yOffset: Int,
    unpackSkipPixels: Int,
    unpackSkipRows: Int,
    width: Int,
    height: Int,
    blur: Boolean,
    clamp: Boolean,
    mipmap: Boolean,
    autoClose: Boolean,
) {
    val filter = if (blur) GL11.GL_LINEAR else GL11.GL_NEAREST
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, if (mipmap) GL11.GL_LINEAR_MIPMAP_LINEAR else filter)
    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter)
    if (clamp) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
    }
    GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, this.width)
    GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, unpackSkipPixels)
    GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, unpackSkipRows)
    GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4)
    GL11.nglTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOffset, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pointer)
    GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0)
    GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0)
    GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0)
    if (autoClose) close()
}

/** Turns the image upside down, row by row. */
fun NativeImage.flipY() {
    val w = width
    val h = height
    for (y in 0 until h / 2) {
        for (x in 0 until w) {
            val top = getPixel(x, y)
            setPixel(x, y, getPixel(x, h - 1 - y))
            setPixel(x, h - 1 - y, top)
        }
    }
}
