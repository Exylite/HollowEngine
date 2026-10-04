package ru.hollowhorizon.hollowengine.client.render.legacy

import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import com.mojang.blaze3d.opengl.GlStateManager as VanillaGl

/**
 * Minecraft 26 dropped the immediate-mode `RenderSystem`/`ShaderInstance`/`BufferBuilder` pipeline
 * the engine was written against: everything is a RenderPipeline drawn by a frame graph now. The
 * engine keeps its own GL programs and buffers, so this package brings back just the part of the
 * old API it used (see [RenderSystem], [ShaderInstance], [BufferBuilder] and friends), running on
 * top of the OpenGL backend that the bootstrap forces.
 *
 * The rules that keep the two worlds from fighting over the context:
 *  - state that vanilla caches (blend, depth, cull, texture bindings, framebuffers) is always set
 *    through vanilla's own GlStateManager, so its cache never lies;
 *  - state vanilla does not cache (program, vertex array, array buffer, viewport, scissor box) is
 *    saved and put back by [scope] around anything that draws.
 */
object LegacyGl {
    private var depth = 0
    private var savedProgram = 0
    private var savedVao = 0
    private var savedArrayBuffer = 0
    private val savedViewport = IntArray(4)
    private val savedScissor = IntArray(4)

    /** Runs [body], then restores what vanilla expects to find on the context. Nesting is free. */
    inline fun <T> scope(body: () -> T): T {
        enter()
        try {
            return body()
        } finally {
            exit()
        }
    }

    // the saved state of nested scopes is simply the state at the outermost entry
    @PublishedApi
    internal fun enter() {
        if (depth++ != 0) return
        savedProgram = GL11.glGetInteger(GL20_CURRENT_PROGRAM)
        savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
        savedArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport)
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, savedScissor)
    }

    @PublishedApi
    internal fun exit() {
        if (--depth != 0) return
        GL33.glUseProgram(savedProgram)
        GL33.glBindVertexArray(savedVao)
        GL33.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedArrayBuffer)
        GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3])
        GL11.glScissor(savedScissor[0], savedScissor[1], savedScissor[2], savedScissor[3])
        RenderSystem.afterScope()
    }

    /** The GL name behind a vanilla texture. */
    fun textureId(texture: AbstractTexture): Int = (texture.texture as GlTexture).glId()

    fun textureId(view: GpuTextureView): Int = (view.texture() as GlTexture).glId()

    /** The light map of the level, as the lit entity shaders sample it. */
    fun lightmapTextureId(): Int = textureId(Minecraft.getInstance().gameRenderer.levelLightmap())

    /** The overlay (hurt flash) texture. */
    fun overlayTextureId(): Int = textureId(Minecraft.getInstance().gameRenderer.overlayTexture().textureView)

    private val rawTextures = HashMap<Identifier, Int>()

    /** Lets the engine's own GL textures be asked for by location like the game's: they are not the game's to own. */
    fun registerRawTexture(location: Identifier, glId: Int) {
        rawTextures[location] = glId
    }

    fun releaseRawTexture(location: Identifier) {
        rawTextures.remove(location)
    }

    fun textureId(location: Identifier): Int =
        rawTextures[location] ?: textureId(Minecraft.getInstance().textureManager.getTexture(location))

    private const val GL20_CURRENT_PROGRAM = 0x8B8D

    fun activeTextureUnit(): Int = GL11.glGetInteger(GL33.GL_ACTIVE_TEXTURE) - GL33.GL_TEXTURE0

    internal fun bindTexture(unit: Int, id: Int) {
        VanillaGl._activeTexture(GL33.GL_TEXTURE0 + unit)
        VanillaGl._bindTexture(id)
        GL33.glBindSampler(unit, 0)
    }
}

/** What `AbstractTexture#getId` used to be. */
val AbstractTexture.id: Int get() = LegacyGl.textureId(this)
