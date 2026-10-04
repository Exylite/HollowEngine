package ru.hollowhorizon.hollowengine.client.render.legacy

import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import ru.hollowhorizon.hollowengine.HollowEngine

/**
 * Vanilla only collects the GUI while screens and HUD layers are extracted and draws it afterwards,
 * which is too late for anything that draws with GL calls as it goes. The engine's UI hands what it
 * would have drawn to [defer] and the bridge runs it from [flush] once vanilla's GUI is on screen.
 *
 * What is drawn here comes over everything vanilla drew in the frame.
 */
object GuiDeferred {
    private val tasks = ArrayList<Runnable>()

    fun defer(task: Runnable) {
        tasks += task
    }

    /** Draws everything that was deferred with the projection the 1.21 GUI had. */
    fun flush() {
        if (tasks.isEmpty()) return
        val pending = ArrayList(tasks)
        tasks.clear()

        val minecraft = Minecraft.getInstance()
        val window = minecraft.window
        LegacyGl.scope {
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
            }
        }
    }
}
