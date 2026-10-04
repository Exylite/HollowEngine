package ru.hollowhorizon.hollowengine.client.render.legacy

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL33
import java.nio.ByteBuffer
import com.mojang.blaze3d.opengl.GlStateManager as VanillaGl

/**
 * The part of the 1.21 `GlStateManager` the engine used. Everything goes through vanilla's own
 * manager so that its state cache and the real context never disagree.
 */
object GlStateManager {
    enum class SourceFactor(val value: Int) {
        CONSTANT_ALPHA(GL33.GL_CONSTANT_ALPHA),
        CONSTANT_COLOR(GL33.GL_CONSTANT_COLOR),
        DST_ALPHA(GL33.GL_DST_ALPHA),
        DST_COLOR(GL33.GL_DST_COLOR),
        ONE(GL33.GL_ONE),
        ONE_MINUS_CONSTANT_ALPHA(GL33.GL_ONE_MINUS_CONSTANT_ALPHA),
        ONE_MINUS_CONSTANT_COLOR(GL33.GL_ONE_MINUS_CONSTANT_COLOR),
        ONE_MINUS_DST_ALPHA(GL33.GL_ONE_MINUS_DST_ALPHA),
        ONE_MINUS_DST_COLOR(GL33.GL_ONE_MINUS_DST_COLOR),
        ONE_MINUS_SRC_ALPHA(GL33.GL_ONE_MINUS_SRC_ALPHA),
        ONE_MINUS_SRC_COLOR(GL33.GL_ONE_MINUS_SRC_COLOR),
        SRC_ALPHA(GL33.GL_SRC_ALPHA),
        SRC_ALPHA_SATURATE(GL33.GL_SRC_ALPHA_SATURATE),
        SRC_COLOR(GL33.GL_SRC_COLOR),
        ZERO(GL33.GL_ZERO),
    }

    enum class DestFactor(val value: Int) {
        CONSTANT_ALPHA(GL33.GL_CONSTANT_ALPHA),
        CONSTANT_COLOR(GL33.GL_CONSTANT_COLOR),
        DST_ALPHA(GL33.GL_DST_ALPHA),
        DST_COLOR(GL33.GL_DST_COLOR),
        ONE(GL33.GL_ONE),
        ONE_MINUS_CONSTANT_ALPHA(GL33.GL_ONE_MINUS_CONSTANT_ALPHA),
        ONE_MINUS_CONSTANT_COLOR(GL33.GL_ONE_MINUS_CONSTANT_COLOR),
        ONE_MINUS_DST_ALPHA(GL33.GL_ONE_MINUS_DST_ALPHA),
        ONE_MINUS_DST_COLOR(GL33.GL_ONE_MINUS_DST_COLOR),
        ONE_MINUS_SRC_ALPHA(GL33.GL_ONE_MINUS_SRC_ALPHA),
        ONE_MINUS_SRC_COLOR(GL33.GL_ONE_MINUS_SRC_COLOR),
        SRC_ALPHA(GL33.GL_SRC_ALPHA),
        SRC_COLOR(GL33.GL_SRC_COLOR),
        ZERO(GL33.GL_ZERO),
    }

    /** `GlStateManager.TEXTURES[unit].binding`: what vanilla believes is bound to a unit. */
    class TextureState(private val unit: Int) {
        val binding: Int get() = VanillaGl.TEXTURES[unit].binding
    }

    object TEXTURES {
        operator fun get(unit: Int) = TextureState(unit)
    }

    /** The active texture unit as an index, like the field of the same name used to be. */
    val activeTexture: Int get() = VanillaGl.activeTexture

    fun _getActiveTexture(): Int = GL33.GL_TEXTURE0 + VanillaGl.activeTexture

    fun _activeTexture(texture: Int) = VanillaGl._activeTexture(texture)
    fun _bindTexture(id: Int) = VanillaGl._bindTexture(id)
    fun _genTexture(): Int = VanillaGl._genTexture()
    fun _deleteTexture(id: Int) = VanillaGl._deleteTexture(id)
    fun _texParameter(target: Int, name: Int, value: Int) = VanillaGl._texParameter(target, name, value)

    fun _texImage2D(
        target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int,
        pixels: ByteBuffer?,
    ) = VanillaGl._texImage2D(target, level, internalFormat, width, height, border, format, type, pixels)

    fun _glUseProgram(program: Int) = VanillaGl._glUseProgram(program)

    fun _glBindFramebuffer(target: Int, framebuffer: Int) = VanillaGl._glBindFramebuffer(target, framebuffer)
    fun glGenFramebuffers(): Int = VanillaGl.glGenFramebuffers()
    fun _glDeleteFramebuffers(framebuffer: Int) = VanillaGl._glDeleteFramebuffers(framebuffer)
    fun _glFramebufferTexture2D(target: Int, attachment: Int, texTarget: Int, texture: Int, level: Int) =
        VanillaGl._glFramebufferTexture2D(target, attachment, texTarget, texture, level)

    fun _enableCull() = VanillaGl._enableCull()
    fun _disableCull() = VanillaGl._disableCull()
    fun _enableBlend() = VanillaGl._enableBlend(0)
    fun _disableBlend() = VanillaGl._disableBlend(0)
    fun _enableDepthTest() = VanillaGl._enableDepthTest()
    fun _disableDepthTest() = VanillaGl._disableDepthTest()
    fun _depthMask(mask: Boolean) = VanillaGl._depthMask(mask)
    fun _depthFunc(func: Int) = VanillaGl._depthFunc(func)
    fun _viewport(x: Int, y: Int, width: Int, height: Int) = VanillaGl._viewport(x, y, width, height)

    fun _blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) =
        VanillaGl._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)

    fun _blendEquation(mode: Int) = VanillaGl._blendEquationSeparate(mode, mode)

    fun _getInteger(name: Int): Int = GL11.glGetInteger(name)
}
