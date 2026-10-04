package ru.hollowhorizon.hollowengine.client.render.legacy

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import ru.hollowhorizon.hollowengine.HollowEngine
import ru.hollowhorizon.hollowengine.client.ui.render.UiGlDiagnostics

/**
 * Vanilla only collects the GUI while screens and HUD layers are extracted and draws it afterwards,
 * which is too late for anything that draws with GL calls as it goes. The engine's UI hands what it
 * would have drawn to [defer] and the bridge runs it from [flush] once vanilla's GUI is on screen.
 *
 * What is drawn here comes over everything vanilla drew in the frame.
 */
object GuiDeferred {
    private val tasks = ArrayList<Runnable>()
    private val vanillaTasks = ArrayList<(GuiGraphicsExtractor) -> Unit>()

    /**
     * How many of vanilla's GUI units one unit of the engine's UI is: the engine's surface may run at a
     * scale of its own, vanilla's items and entities are placed in the scale the window has.
     */
    @JvmField
    var vanillaUnitsPerUiUnit = 1f

    fun defer(task: Runnable) {
        tasks += task
    }

    /**
     * Things only vanilla can draw (items, entities, tooltips) that the engine's UI places inside its own
     * frame. They go through vanilla's GUI once more after the engine's frame, so they come over it.
     */
    fun deferVanilla(task: (GuiGraphicsExtractor) -> Unit) {
        vanillaTasks += task
    }

    /** Runs what the engine deferred, then what it deferred to vanilla. */
    fun flushAll(minecraft: Minecraft) {
        flush()
        flushVanilla(minecraft)
    }

    private fun flushVanilla(minecraft: Minecraft) {
        if (vanillaTasks.isEmpty()) return
        val pending = ArrayList(vanillaTasks)
        vanillaTasks.clear()
        val renderer = minecraft.gameRenderer
        val window = minecraft.window
        val mouseX = minecraft.mouseHandler.getScaledXPos(window).toInt()
        val mouseY = minecraft.mouseHandler.getScaledYPos(window).toInt()
        val graphics = GuiGraphicsExtractor(minecraft, renderer.gameRenderState().guiRenderState, mouseX, mouseY)
        for (task in pending) {
            try {
                task(graphics)
            } catch (e: Throwable) {
                HollowEngine.LOGGER.error("A deferred vanilla GUI draw failed", e)
            }
        }
        // the first pass emptied the GUI state, so this draws only what was just extracted
        renderer.guiRenderer.render()
    }

    /** Draws everything that was deferred with the projection the 1.21 GUI had. */
    fun flush() {
        if (tasks.isEmpty()) return
        val pending = ArrayList(tasks)
        tasks.clear()

        val minecraft = Minecraft.getInstance()
        val window = minecraft.window
        LegacyGl.scope {
            UiGlDiagnostics.beginFrame()
            // switches other mods leave on and vanilla never looks at: the UI would be clipped or recoloured by them
            val stray = BooleanArray(StrayStates.size) { GL11.glIsEnabled(StrayStates[it]) }
            for (i in StrayStates.indices) if (stray[i]) GL11.glDisable(StrayStates[i])
            val target = MainTarget.get()
            target.bindWrite(true)
            RenderSystem.reverseDepth = false
            RenderSystem.viewport(0, 0, window.width, window.height)
            GL11.glClearDepth(1.0)
            RenderSystem.depthMask(true)
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()
            RenderSystem.enableDepthTest()
            RenderSystem.setProjectionMatrix(
                Matrix4f().setOrtho(
                    0f, (window.width / window.guiScale).toFloat(), (window.height / window.guiScale).toFloat(), 0f,
                    1000f, 21000f,
                ),
                VertexSorting.ORTHOGRAPHIC_Z,
            )
            val modelView = RenderSystem.getModelViewStack()
            modelView.pushMatrix()
            modelView.translation(0f, 0f, -11000f)
            RenderSystem.applyModelViewMatrix()
            try {
                for (task in pending) {
                    try {
                        task.run()
                    } catch (e: Throwable) {
                        HollowEngine.LOGGER.error("A deferred GUI draw failed", e)
                    }
                }
            } finally {
                modelView.popMatrix()
                RenderSystem.applyModelViewMatrix()
                RenderSystem.enableCull()
                RenderSystem.defaultBlendFunc()
                for (i in StrayStates.indices) if (stray[i]) GL11.glEnable(StrayStates[i])
                UiGlDiagnostics.endFrame()
            }
        }
    }

    private val StrayStates = intArrayOf(GL11.GL_STENCIL_TEST, GL30.GL_FRAMEBUFFER_SRGB, GL11.GL_COLOR_LOGIC_OP)
}
