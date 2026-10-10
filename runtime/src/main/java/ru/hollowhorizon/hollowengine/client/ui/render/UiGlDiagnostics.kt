package ru.hollowhorizon.hollowengine.client.ui.render

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryStack
import ru.hollowhorizon.hollowengine.HollowEngine

/**
 * Tells GL errors that the UI frame raised apart from the ones that were already pending on the context,
 * and says what state the frame ran in. The UI shares the context with vanilla and with other mods, so a
 * report like "the UI does not show" needs this to tell whose state it is.
 *
 * Only the first few reports are logged. `-Dhollowengine.ui.debug` also logs the state a frame runs in, once a second.
 */
internal object UiGlDiagnostics {
    private const val MaxReports = 6
    private const val MaxErrorsPerDrain = 16
    private const val SnapshotIntervalNanos = 1_000_000_000L

    private val verbose = System.getProperty("hollowengine.ui.debug") != null
    private var reports = 0
    private var lastSnapshotNanos = 0L

    fun beginFrame() {
        val stale = drain()
        if (stale.isNotEmpty()) {
            report("GL errors were already pending before the UI frame (vanilla or another mod left them): ${names(stale)}")
        }
        if (verbose) {
            val now = System.nanoTime()
            if (now - lastSnapshotNanos >= SnapshotIntervalNanos) {
                lastSnapshotNanos = now
                HollowEngine.LOGGER.info("UI frame GL state: {}", snapshot())
            }
        }
    }

    fun endFrame() {
        val errors = drain()
        if (errors.isNotEmpty()) report("GL errors during the UI frame: ${names(errors)}; state: ${snapshot()}")
    }

    private fun drain(): List<Int> {
        val first = GL11.glGetError()
        if (first == GL11.GL_NO_ERROR) return emptyList()
        val errors = arrayListOf(first)
        while (errors.size < MaxErrorsPerDrain) {
            val error = GL11.glGetError()
            if (error == GL11.GL_NO_ERROR) break
            errors += error
        }
        return errors
    }

    private fun report(message: String) {
        if (reports > MaxReports) return
        reports++
        HollowEngine.LOGGER.warn(if (reports > MaxReports) "Further UI GL reports are not logged" else message)
    }

    private fun names(errors: List<Int>) = errors.joinToString { name(it) }

    private fun name(error: Int) = when (error) {
        GL11.GL_INVALID_ENUM -> "INVALID_ENUM"
        GL11.GL_INVALID_VALUE -> "INVALID_VALUE"
        GL11.GL_INVALID_OPERATION -> "INVALID_OPERATION"
        GL30.GL_INVALID_FRAMEBUFFER_OPERATION -> "INVALID_FRAMEBUFFER_OPERATION"
        GL11.GL_OUT_OF_MEMORY -> "OUT_OF_MEMORY"
        else -> "0x" + Integer.toHexString(error)
    }

    private fun snapshot(): String = MemoryStack.stackPush().use { stack ->
        val viewport = stack.mallocInt(4).also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }
        val scissor = stack.mallocInt(4).also { GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, it) }
        val mask = stack.malloc(4).also { GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, it) }
        val polygon = stack.mallocInt(2).also { GL11.glGetIntegerv(GL11.GL_POLYGON_MODE, it) }
        "drawFbo=${GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)} readFbo=${GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)} " +
                "viewport=[${viewport[0]}, ${viewport[1]}, ${viewport[2]}, ${viewport[3]}] " +
                "scissor=${GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)}[${scissor[0]}, ${scissor[1]}, ${scissor[2]}, ${scissor[3]}] " +
                "stencil=${GL11.glIsEnabled(GL11.GL_STENCIL_TEST)} srgb=${GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB)} " +
                "logicOp=${GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP)} cull=${GL11.glIsEnabled(GL11.GL_CULL_FACE)} " +
                "blend=${GL11.glIsEnabled(GL11.GL_BLEND)} depth=${GL11.glIsEnabled(GL11.GL_DEPTH_TEST)} " +
                "colorMask=[${mask[0].toInt() != 0}, ${mask[1].toInt() != 0}, ${mask[2].toInt() != 0}, ${mask[3].toInt() != 0}] " +
                "polygonMode=0x${Integer.toHexString(polygon[0])} program=${GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)} " +
                "vao=${GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)} activeTexture=${GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) - GL13.GL_TEXTURE0} " +
                "renderer=${GL11.glGetString(GL11.GL_RENDERER)} version=${GL11.glGetString(GL11.GL_VERSION)}"
    }
}
