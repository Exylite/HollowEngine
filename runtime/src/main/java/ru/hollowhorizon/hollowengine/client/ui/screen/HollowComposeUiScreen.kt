package ru.hollowhorizon.hollowengine.client.ui.screen

import androidx.compose.runtime.Composable
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import ru.hollowhorizon.hollowengine.client.render.legacy.GuiDeferred
import ru.hollowhorizon.hollowengine.client.render.legacy.RenderSystem
import ru.hollowhorizon.hollowengine.common.utils.compat.mainRenderTarget
import ru.hollowhorizon.hollowengine.common.utils.compat.window
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import org.lwjgl.glfw.GLFW
import ru.hollowhorizon.hollowengine.client.ui.*
import ru.hollowhorizon.hollowengine.client.ui.render.MinecraftUiRenderer
import ru.hollowhorizon.hollowengine.client.ui.render.UiRenderTarget
import ru.hollowhorizon.hollowengine.client.ui.style.CompiledHss
import ru.hollowhorizon.hollowengine.client.utils.mc
import ru.hollowhorizon.hollowengine.client.utils.popPose
import ru.hollowhorizon.hollowengine.client.utils.pushPose
import ru.hollowhorizon.hollowengine.common.ui.UiGuiScale
import ru.hollowhorizon.hollowengine.common.utils.literal
import kotlin.math.ceil


abstract class HollowComposeUiScreen(
    title: String,
    stylesheet: CompiledHss,
) : Screen(title.literal) {
    private val surface = HollowUiSurface(stylesheet = stylesheet)
    private val renderer = MinecraftUiRenderer()
    private val pipeline = PipelinedUiFrameBuilder()

    @Composable
    protected abstract fun Content()

    protected open fun rebuildEveryFrame(): Boolean = false

    /**
     * Puts the whole tree in [UiState.CLOSING] so the stylesheet's `:closing` rules apply.
     */
    protected var closing: Boolean
        get() = surface.isClosing
        set(value) {
            surface.isClosing = value
        }

    /** The scale this screen lays itself out at; [UiGuiScale.Inherit] follows the player's setting. */
    protected open fun guiScale(): UiGuiScale = UiGuiScale.Inherit

    /**
     * Opt-in frame pipelining: the next frame's build (recomposition, style resolve, layout) runs on
     * a background thread while the game renders, and is consumed one frame later.
     */
    protected open fun pipelineFrames(): Boolean = false

    /**
     * Hands vanilla something to draw with a [GuiGraphicsExtractor] alongside the UI frame.
     *
     * The engine's own screen-render events are posted from a mixin on `Screen.extractRenderState`, which
     * this class overrides without calling through, so they never fire here. Content that needs vanilla
     * drawing (item tooltips, for one) hooks in from this method instead. It is drawn by vanilla's GUI
     * once more after the engine's own frame, see [GuiDeferred.deferVanilla].
     */
    protected open fun renderAfterUi(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) = Unit

    protected fun focusInput(nodeId: String) {
        pipeline.await()
        surface.runtime.focus(nodeId)
    }

    override fun init() {
        // init() also runs on window resize while a build may be in flight.
        pipeline.reset()
        surface.setContent { Content() }
    }

    /** The surface's own logical size, and how it relates to vanilla's GUI pixels. */
    private class SurfaceScale(val width: Float, val height: Float, val ratio: Float)

    private fun surfaceScale(): SurfaceScale? {
        val window = mc.window
        val factor = when (val scale = guiScale()) {
            UiGuiScale.Inherit -> return null
            UiGuiScale.Auto -> window.calculateScale(0, mc.isEnforceUnicode)
            is UiGuiScale.Fixed -> window.calculateScale(scale.factor, mc.isEnforceUnicode)
        }.coerceAtLeast(1)
        if (factor == window.guiScale) return null

        val logicalWidth = ceil(window.width.toDouble() / factor).toFloat().coerceAtLeast(1f)
        val logicalHeight = ceil(window.height.toDouble() / factor).toFloat().coerceAtLeast(1f)
        val vanillaWidth = window.guiScaledWidth.toFloat()
        if (vanillaWidth <= 0f) return null
        return SurfaceScale(logicalWidth, logicalHeight, logicalWidth / vanillaWidth)
    }

    /** The engine's frame is the whole background; vanilla's dimming and blur are not wanted under it. */
    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) = Unit

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val scale = surfaceScale()
        val frameWidth = scale?.width ?: width.toFloat()
        val frameHeight = scale?.height ?: height.toFloat()
        val ratio = scale?.ratio ?: 1f
        val pointerX = mouseX.toFloat() * ratio
        val pointerY = mouseY.toFloat() * ratio
        val frame = (if (pipelineFrames()) pipeline.take(frameWidth, frameHeight) else null)
            ?: buildFrame(frameWidth, frameHeight, pointerX, pointerY, System.nanoTime())
        // vanilla draws its GUI after the whole frame is extracted: the engine draws with GL calls, so it waits for that
        GuiDeferred.defer { renderScaled(frame, scale) }
        // vanilla's own content that belongs over the engine's frame (item tooltips, for one) gets its own pass
        GuiDeferred.deferVanilla { over ->
            renderAfterUi(over, mouseX, mouseY)
            over.extractDeferredElements(mouseX, mouseY, partialTick)
        }
        UiCursorManager.claim(mc.window.window, this, surface.runtime.cursor, UiCursorManager.ScreenPriority)
        if (pipelineFrames()) {
            pipeline.schedule(frameWidth, frameHeight) {
                buildFrame(frameWidth, frameHeight, pointerX, pointerY, System.nanoTime())
            }
        }
    }

    /**
     * A screen at its own scale draws through a render target whose logical size is the surface's,
     * which is what re-maps the projection.
     */
    private fun renderScaled(frame: HollowUiFrame, scale: SurfaceScale?) {
        if (scale == null) {
            renderer.render(frame)
            return
        }

        val window = mc.window
        val target = UiRenderTarget(
            framebufferId = mc.mainRenderTarget.frameBufferId,
            x = 0,
            y = 0,
            width = window.width,
            height = window.height,
            logicalWidth = scale.width,
            logicalHeight = scale.height,
            scale = window.width / scale.width,
        )

        val projection = RenderSystem.getProjectionMatrix()
        val sorting = RenderSystem.getVertexSorting()
        RenderSystem.getModelViewStack().pushPose()
        try {
            renderer.render(frame, target)
        } finally {
            RenderSystem.setProjectionMatrix(projection, sorting)
            RenderSystem.getModelViewStack().popPose()
            RenderSystem.applyModelViewMatrix()
            mc.mainRenderTarget.bindWrite(true)
        }
    }

    private fun buildFrame(width: Float, height: Float, mouseX: Float, mouseY: Float, nowNanos: Long): HollowUiFrame {
        if (rebuildEveryFrame()) surface.advanceFrameTime(nowNanos)
        return surface.frame(width, height, mouseX, mouseY, nowNanos)
    }

    override fun removed() {
        UiCursorManager.release(mc.window.window, this)
        pipeline.reset()
        renderer.close()
        surface.close()
        super.removed()
    }

    /** Vanilla hands pointer positions in its own GUI pixels; the surface thinks in its own. */
    private fun Double.toSurface(): Float = (this * (surfaceScale()?.ratio ?: 1f)).toFloat()

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (closing) return false
        pipeline.await()
        return surface.runtime.mouseClicked(event.x.toSurface(), event.y.toSurface(), event.button(), currentUiKeyModifiers())
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (closing) return false
        pipeline.await()
        return surface.runtime.mouseReleased(event.x.toSurface(), event.y.toSurface(), event.button(), currentUiKeyModifiers())
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (closing) return false
        pipeline.await()
        return surface.runtime.mouseDragged(
            event.x.toSurface(), event.y.toSurface(), event.button(), dragX.toSurface(), dragY.toSurface(),
            currentUiKeyModifiers(),
        )
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (closing) return false
        pipeline.await()
        return surface.runtime.mouseScrolled(
            mouseX.toSurface(),
            mouseY.toSurface(),
            scrollX.toFloat(),
            scrollY.toFloat(),
            currentUiKeyModifiers(),
        )
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (closing) return false
        if (super.charTyped(event)) return true
        pipeline.await()
        val modifiers = currentUiKeyModifiers()
        // the UI takes UTF-16 units, like 1.21 handed them out
        var handled = false
        for (unit in Character.toChars(event.codepoint)) handled = surface.runtime.charTyped(unit, modifiers) || handled
        return handled
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (closing) return false
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            pipeline.await()
            if (surface.runtime.keyConsumed(event.key(), event.scancode(), event.modifiers())) return true
            return super.keyPressed(event)
        }
        if (super.keyPressed(event)) return true
        pipeline.await()
        return surface.runtime.keyPressed(event.key(), event.scancode(), event.modifiers())
    }

    override fun isPauseScreen(): Boolean = false
}
