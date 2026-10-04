package ru.hollowhorizon.hollowengine.client.render.legacy

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Matrix4fStack
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import java.util.function.Supplier
import com.mojang.blaze3d.opengl.GlStateManager as VanillaGl

/**
 * The 1.21 `RenderSystem`, for the engine's own drawing. It keeps the matrices, the current shader
 * and the shader textures itself and pushes state through vanilla's GlStateManager, so it can be
 * used inside the vanilla frame without confusing it.
 */
object RenderSystem {
    private val modelViewStack = Matrix4fStack(16)
    private var modelViewMatrix = Matrix4f()
    private var projectionMatrix = Matrix4f()
    private val projectionBackup = ArrayDeque<Matrix4f>()
    private var shader: ShaderInstance? = null
    private val shaderTextures = IntArray(12)
    private val shaderColor = floatArrayOf(1f, 1f, 1f, 1f)

    @JvmField var fogStart = 1.0e8f
    @JvmField var fogEnd = 1.1e8f
    @JvmField var fogColor = floatArrayOf(0f, 0f, 0f, 0f)
    @JvmField var fogShape = 0
    @JvmField var lineWidth = 1f
    @JvmField var gameTime = 0f
    @JvmField val light0 = Vector3f(0.2f, 1.0f, -0.7f).normalize()
    @JvmField val light1 = Vector3f(-0.2f, 1.0f, 0.7f).normalize()

    /**
     * Whether depth is compared the way the level is drawn in 26.x, with the far plane at 0. The
     * world stage hooks turn it on; everything with an orthographic matrix of its own leaves it off.
     */
    @JvmField var reverseDepth = false

    val screenWidth: Int get() = Minecraft.getInstance().window.width
    val screenHeight: Int get() = Minecraft.getInstance().window.height

    // matrices

    @JvmStatic fun getModelViewStack(): Matrix4fStack = modelViewStack

    @JvmStatic fun getModelViewMatrix(): Matrix4f = modelViewMatrix

    @JvmStatic fun applyModelViewMatrix() {
        modelViewMatrix = Matrix4f(modelViewStack)
    }

    @JvmStatic fun getProjectionMatrix(): Matrix4f = projectionMatrix

    @JvmStatic fun setProjectionMatrix(matrix: Matrix4f, sorting: VertexSorting = VertexSorting.ORTHOGRAPHIC_Z) {
        projectionMatrix = Matrix4f(matrix)
    }

    @JvmStatic fun getVertexSorting(): VertexSorting = VertexSorting.ORTHOGRAPHIC_Z

    @JvmStatic fun backupProjectionMatrix() {
        projectionBackup.addLast(Matrix4f(projectionMatrix))
    }

    @JvmStatic fun restoreProjectionMatrix() {
        projectionBackup.removeLastOrNull()?.let { projectionMatrix = it }
    }

    // shader

    @JvmStatic fun setShader(supplier: Supplier<ShaderInstance?>) {
        shader = supplier.get()
    }

    @JvmStatic fun setShader(instance: ShaderInstance?) {
        shader = instance
    }

    @JvmStatic fun getShader(): ShaderInstance? = shader

    @JvmStatic fun setShaderTexture(unit: Int, id: Int) {
        if (unit in shaderTextures.indices) shaderTextures[unit] = id
    }

    @JvmStatic fun setShaderTexture(unit: Int, location: Identifier) {
        setShaderTexture(unit, LegacyGl.textureId(location))
    }

    @JvmStatic fun getShaderTexture(unit: Int): Int = if (unit in shaderTextures.indices) shaderTextures[unit] else 0

    @JvmStatic fun getShaderColor(): FloatArray = shaderColor

    @JvmStatic fun setShaderColor(r: Float, g: Float, b: Float, a: Float) {
        shaderColor[0] = r; shaderColor[1] = g; shaderColor[2] = b; shaderColor[3] = a
    }

    /** Back to the lights the level is drawn with, which is what the engine's shaders expect when nothing sets others. */
    @JvmStatic fun resetShaderLights() {
        light0.set(Vector3f(0.2f, 1.0f, -0.7f).normalize())
        light1.set(Vector3f(-0.2f, 1.0f, 0.7f).normalize())
    }

    @JvmStatic fun setShaderLights(first: Vector3f, second: Vector3f) {
        light0.set(first)
        light1.set(second)
    }

    // state, all of it through vanilla's cache

    @JvmStatic fun enableBlend() = VanillaGl._enableBlend(0)
    @JvmStatic fun disableBlend() = VanillaGl._disableBlend(0)

    @JvmStatic fun defaultBlendFunc() = blendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
    )

    @JvmStatic fun blendFunc(src: GlStateManager.SourceFactor, dst: GlStateManager.DestFactor) =
        VanillaGl._blendFuncSeparate(src.value, dst.value, src.value, dst.value)

    @JvmStatic fun blendFunc(src: Int, dst: Int) = VanillaGl._blendFuncSeparate(src, dst, src, dst)

    @JvmStatic fun blendFuncSeparate(
        srcRgb: GlStateManager.SourceFactor, dstRgb: GlStateManager.DestFactor,
        srcAlpha: GlStateManager.SourceFactor, dstAlpha: GlStateManager.DestFactor,
    ) = VanillaGl._blendFuncSeparate(srcRgb.value, dstRgb.value, srcAlpha.value, dstAlpha.value)

    @JvmStatic fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) =
        VanillaGl._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)

    @JvmStatic fun blendEquation(mode: Int) = VanillaGl._blendEquationSeparate(mode, mode)

    @JvmStatic fun enableDepthTest() {
        VanillaGl._enableDepthTest()
        VanillaGl._depthFunc(if (reverseDepth) GL11.GL_GEQUAL else GL11.GL_LEQUAL)
    }

    @JvmStatic fun disableDepthTest() = VanillaGl._disableDepthTest()
    @JvmStatic fun depthMask(mask: Boolean) = VanillaGl._depthMask(mask)
    @JvmStatic fun depthFunc(func: Int) = VanillaGl._depthFunc(func)
    @JvmStatic fun enableCull() = VanillaGl._enableCull()
    @JvmStatic fun disableCull() = VanillaGl._disableCull()

    @JvmStatic fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        VanillaGl._colorMask(
            (if (red) 1 else 0) or (if (green) 2 else 0) or (if (blue) 4 else 0) or (if (alpha) 8 else 0)
        )
    }

    @JvmStatic fun viewport(x: Int, y: Int, width: Int, height: Int) = VanillaGl._viewport(x, y, width, height)

    @JvmStatic fun lineWidth(width: Float) {
        lineWidth = width
        GL11.glLineWidth(width)
    }

    @JvmStatic fun activeTexture(texture: Int) = VanillaGl._activeTexture(texture)

    @JvmStatic fun bindTexture(id: Int) {
        VanillaGl._bindTexture(id)
        GL33.glBindSampler(VanillaGl.activeTexture, 0)
    }

    @JvmStatic fun texParameter(target: Int, name: Int, value: Int) = VanillaGl._texParameter(target, name, value)

    // things vanilla leaves uncached: scopes put them back

    @JvmStatic fun glBindVertexArray(vao: Int) = GL30.glBindVertexArray(vao)
    @JvmStatic fun glBindVertexArray(vao: Supplier<Int>) = GL30.glBindVertexArray(vao.get())
    @JvmStatic fun glBindBuffer(target: Int, buffer: Int) = GL15.glBindBuffer(target, buffer)
    @JvmStatic fun glBindBuffer(target: Int, buffer: Supplier<Int>) = GL15.glBindBuffer(target, buffer.get())
    @JvmStatic fun glUniform1i(location: Int, value: Int) = org.lwjgl.opengl.GL20.glUniform1i(location, value)

    // threading

    @JvmStatic fun isOnRenderThread(): Boolean = com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()
    @JvmStatic fun isOnRenderThreadOrInit(): Boolean = com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()
    @JvmStatic fun assertOnRenderThread() = com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread()

    @JvmStatic fun recordRenderCall(call: Runnable) {
        if (isOnRenderThread()) call.run() else Minecraft.getInstance().execute(call)
    }

    @JvmStatic fun runAsFancy(body: Runnable) = body.run()

    internal fun afterScope() {}
}
