package ru.hollowhorizon.hollowengine.client.render.legacy

import com.mojang.blaze3d.opengl.GlTexture
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer
import com.mojang.blaze3d.opengl.GlStateManager as VanillaGl

/**
 * A framebuffer with a colour texture and, if asked for, a depth texture; the 1.21 `RenderTarget`,
 * over plain GL objects that vanilla knows nothing about.
 */
open class RenderTarget(@JvmField val useDepth: Boolean, @JvmField val depthFormat: Int = GL14.GL_DEPTH_COMPONENT24) {
    @JvmField var width = 0
    @JvmField var height = 0
    @JvmField var viewWidth = 0
    @JvmField var viewHeight = 0
    @JvmField var frameBufferId = -1
    @JvmField var colorTextureId = -1
    @JvmField var depthTextureId = -1
    @JvmField var clearChannels = floatArrayOf(1f, 1f, 1f, 0f)
    private var filterMode = GL11.GL_LINEAR

    open fun resize(width: Int, height: Int) {
        if (frameBufferId >= 0) destroyBuffers()
        createBuffers(width, height)
    }

    open fun createBuffers(width: Int, height: Int) {
        this.width = width
        this.height = height
        viewWidth = width
        viewHeight = height
        frameBufferId = VanillaGl.glGenFramebuffers()
        colorTextureId = VanillaGl._genTexture()
        VanillaGl._bindTexture(colorTextureId)
        VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE)
        VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE)
        VanillaGl._texImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?
        )
        if (useDepth) {
            depthTextureId = VanillaGl._genTexture()
            VanillaGl._bindTexture(depthTextureId)
            VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE)
            VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE)
            VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE)
            VanillaGl._texImage2D(
                GL11.GL_TEXTURE_2D, 0, depthFormat, width, height, 0, GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT, null as ByteBuffer?
            )
        }
        val previous = VanillaGl.getFrameBuffer(GL30.GL_DRAW_FRAMEBUFFER)
        VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferId)
        VanillaGl._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTextureId, 0)
        if (useDepth) {
            VanillaGl._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTextureId, 0)
        }
        VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous)
        VanillaGl._bindTexture(0)
    }

    open fun destroyBuffers() {
        if (depthTextureId > -1) VanillaGl._deleteTexture(depthTextureId)
        if (colorTextureId > -1) VanillaGl._deleteTexture(colorTextureId)
        if (frameBufferId > -1) VanillaGl._glDeleteFramebuffers(frameBufferId)
        depthTextureId = -1
        colorTextureId = -1
        frameBufferId = -1
    }

    fun setFilterMode(mode: Int) {
        filterMode = mode
        VanillaGl._bindTexture(colorTextureId)
        VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode)
        VanillaGl._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode)
        VanillaGl._bindTexture(0)
    }

    fun setClearColor(r: Float, g: Float, b: Float, a: Float) {
        clearChannels = floatArrayOf(r, g, b, a)
    }

    open fun bindRead() {
        VanillaGl._bindTexture(colorTextureId)
    }

    open fun unbindRead() {
        VanillaGl._bindTexture(0)
    }

    open fun bindWrite(setViewport: Boolean) {
        VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferId)
        if (setViewport) VanillaGl._viewport(0, 0, viewWidth, viewHeight)
    }

    open fun unbindWrite() {
        VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
    }

    /** Clears colour and depth; the depth to the legacy far value, since these targets are legacy-depth. */
    open fun clear() {
        val previous = VanillaGl.getFrameBuffer(GL30.GL_DRAW_FRAMEBUFFER)
        bindWrite(true)
        GL11.glClearColor(clearChannels[0], clearChannels[1], clearChannels[2], clearChannels[3])
        var mask = GL11.GL_COLOR_BUFFER_BIT
        if (useDepth) {
            GL11.glClearDepth(1.0)
            mask = mask or GL11.GL_DEPTH_BUFFER_BIT
        }
        VanillaGl._depthMask(true)
        VanillaGl._colorMask(15)
        GL11.glClear(mask)
        VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous)
    }
}

class TextureTarget(width: Int, height: Int, useDepth: Boolean, onOsx: Boolean = false, depthFormat: Int = GL14.GL_DEPTH_COMPONENT24) :
    RenderTarget(useDepth, depthFormat) {
    init {
        resize(width, height)
        clear()
    }
}

/**
 * The target vanilla draws the frame into, seen as one of ours: its textures are wrapped in a
 * framebuffer of our own, since vanilla keeps its framebuffers in a cache that is not for use.
 *
 * Vanilla creates the textures anew whenever the frame changes size (a window resize, fullscreen, a mod
 * that changes the size of the game view), and GL may hand the freed names out again: the colour and depth
 * names trade places the first time and come back unchanged the next. Attaching "the new textures" by name
 * then changes nothing, and the wrapper keeps drawing into the old, orphaned ones: whatever the engine
 * draws through it (UI screens, level hooks) is lost, and GL reports no error. So the wrapper follows the
 * texture objects and not their names, and gets a framebuffer of its own for every new pair.
 */
object MainTarget {
    private val view = object : RenderTarget(true) {
        private var wrappedColor: GlTexture? = null
        private var wrappedDepth: GlTexture? = null

        fun refresh() {
            val vanilla = Minecraft.getInstance().gameRenderer.mainRenderTarget()
            val colorTexture = vanilla.colorTexture as GlTexture
            val depthTexture = vanilla.depthTexture as? GlTexture
            val color = colorTexture.glId()
            val depth = depthTexture?.glId() ?: -1
            width = vanilla.width
            height = vanilla.height
            viewWidth = width
            viewHeight = height
            colorTextureId = color
            depthTextureId = depth
            if (frameBufferId < 0 || colorTexture !== wrappedColor || depthTexture !== wrappedDepth) {
                if (frameBufferId >= 0) VanillaGl._glDeleteFramebuffers(frameBufferId)
                frameBufferId = VanillaGl.glGenFramebuffers()
                val previous = VanillaGl.getFrameBuffer(GL30.GL_DRAW_FRAMEBUFFER)
                VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferId)
                VanillaGl._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, color, 0)
                VanillaGl._glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, if (depth < 0) 0 else depth, 0
                )
                VanillaGl._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous)
                wrappedColor = colorTexture
                wrappedDepth = depthTexture
            }
        }

        override fun createBuffers(width: Int, height: Int) {}
        override fun destroyBuffers() {}
        override fun resize(width: Int, height: Int) {}
        override fun clear() {}
    }

    /** The main target, with its textures and framebuffer as they are right now. */
    @JvmStatic
    fun get(): RenderTarget {
        view.refresh()
        return view
    }
}

/**
 * The internal format of this target's depth texture. A depth blit between targets needs the formats
 * to be the same, and vanilla's depth is not the 24 bits the engine's own targets have.
 */
fun RenderTarget.depthInternalFormat(): Int {
    if (!useDepth || depthTextureId < 0) return depthFormat
    VanillaGl._bindTexture(depthTextureId)
    val format = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT)
    VanillaGl._bindTexture(0)
    return format
}
