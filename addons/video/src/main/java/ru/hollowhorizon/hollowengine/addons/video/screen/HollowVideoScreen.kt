package ru.hollowhorizon.hollowengine.addons.video.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import ru.hollowhorizon.hollowengine.addons.video.playback.VideoPlayerSession
import ru.hollowhorizon.hollowengine.api.VideoPlaybackOptions
import ru.hollowhorizon.hollowengine.client.render.legacy.BufferUploader
import ru.hollowhorizon.hollowengine.client.render.legacy.DefaultVertexFormat
import ru.hollowhorizon.hollowengine.client.render.legacy.GuiDeferred
import ru.hollowhorizon.hollowengine.client.render.legacy.RenderSystem
import ru.hollowhorizon.hollowengine.client.render.legacy.Tesselator
import ru.hollowhorizon.hollowengine.client.render.legacy.VertexFormat
import ru.hollowhorizon.hollowengine.common.registry.ModShaders
import ru.hollowhorizon.hollowengine.common.utils.compat.setScreen
import ru.hollowhorizon.hollowengine.common.utils.literal
import kotlin.math.min
import kotlin.math.roundToInt

class HollowVideoScreen(
    private val session: VideoPlayerSession,
    private val options: VideoPlaybackOptions = VideoPlaybackOptions(),
    private val onClosed: () -> Unit = {},
) : Screen("".literal) {
    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, BackgroundColor)
        // the video is a texture of the engine's own: it is drawn with GL calls once vanilla's GUI is on screen
        session.texture?.let { texture -> GuiDeferred.defer { drawVideo(texture) } }
        if (session.error != null || (options.closeOnEnd && session.ended)) {
            closeScreen()
        }
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) = Unit

    override fun shouldCloseOnEsc(): Boolean = true

    override fun isPauseScreen(): Boolean = false

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            if (session.playing) session.pause() else session.play()
            return true
        }
        return super.keyPressed(event)
    }

    override fun removed() {
        session.close()
        onClosed()
        super.removed()
    }

    private fun drawVideo(texture: Identifier) {
        val videoWidth = session.videoWidth
        val videoHeight = session.videoHeight
        if (videoWidth <= 0 || videoHeight <= 0) return
        val shader = ModShaders.POSITION_TEX ?: return

        val scale = min(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
        val drawWidth = (videoWidth * scale).roundToInt()
        val drawHeight = (videoHeight * scale).roundToInt()
        val left = ((width - drawWidth) / 2).toFloat()
        val top = ((height - drawHeight) / 2).toFloat()

        RenderSystem.setShader(shader)
        RenderSystem.setShaderTexture(0, texture)
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.addVertex(left, top + drawHeight, 0f).setUv(0f, 1f)
        builder.addVertex(left + drawWidth, top + drawHeight, 0f).setUv(1f, 1f)
        builder.addVertex(left + drawWidth, top, 0f).setUv(1f, 0f)
        builder.addVertex(left, top, 0f).setUv(0f, 0f)
        BufferUploader.drawWithShader(builder.buildOrThrow())
    }

    private fun closeScreen() {
        Minecraft.getInstance().setScreen(null)
    }
}

private const val BackgroundColor = 0xFF000000.toInt()
