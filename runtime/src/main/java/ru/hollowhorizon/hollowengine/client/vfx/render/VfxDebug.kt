package ru.hollowhorizon.hollowengine.client.vfx.render

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30
import ru.hollowhorizon.hollowengine.HollowEngine

/**
 * What to look at when effects do not show up: `-Dhollowengine.vfx.debug` logs, about once a second, what a
 * frame holds and the GL state it is drawn in, and every GL error the effect renderers leave behind.
 */
object VfxDebug {
    private val enabled = System.getProperty("hollowengine.vfx.debug") != null
    private val lastReport = HashMap<String, Long>()

    fun report(stage: String, list: VfxDrawList) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - (lastReport[stage] ?: 0L) < 1000) return
        lastReport[stage] = now
        val viewport = IntArray(4).also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }
        val colorMask = BooleanArray(4).let { mask ->
            val buffer = org.lwjgl.BufferUtils.createByteBuffer(4)
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, buffer)
            List(4) { buffer.get(it).toInt() != 0 }.also { mask.fill(true) }
        }
        val depthRange = FloatArray(2).also { GL11.glGetFloatv(GL11.GL_DEPTH_RANGE, it) }
        HollowEngine.LOGGER.info(
            "VFX {}: quads={} meshes={} ribbons={} skies={} posts={} fbo={} viewport={} depthTest={} depthFunc={} depthMask={} " +
                    "colorMask={} cull={} blend={} scissor={} stencil={} clipOrigin={} clipDepth={} depthRange={}",
            stage, list.quads.size, list.meshes.size, list.ribbons.size, list.skies.size, list.posts.size,
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING), viewport.toList(), GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
            Integer.toHexString(GL11.glGetInteger(GL11.GL_DEPTH_FUNC)), GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
            colorMask, GL11.glIsEnabled(GL11.GL_CULL_FACE), GL11.glIsEnabled(GL11.GL_BLEND), GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
            GL11.glIsEnabled(GL11.GL_STENCIL_TEST), Integer.toHexString(GL11.glGetInteger(0x935C)),
            Integer.toHexString(GL11.glGetInteger(0x935D)), depthRange.toList(),
        )
    }

    fun check(stage: String) {
        if (!enabled) return
        val error = GL11.glGetError()
        if (error != GL11.GL_NO_ERROR) HollowEngine.LOGGER.warn("VFX {} left GL error {}", stage, Integer.toHexString(error))
    }

    /** Runs [draw] and logs how many fragments of it passed the depth test, about once a second per [label]. */
    inline fun counted(label: String, draw: () -> Unit) {
        if (!isEnabled()) return draw()
        val query = GL15.glGenQueries()
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, query)
        draw()
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED)
        logSamples(label, GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT))
        GL15.glDeleteQueries(query)
    }

    fun isEnabled() = enabled

    fun logSamples(label: String, samples: Int) {
        val now = System.currentTimeMillis()
        if (now - (lastReport["samples $label"] ?: 0L) < 1000) return
        lastReport["samples $label"] = now
        HollowEngine.LOGGER.info("VFX {} samples passed: {}", label, samples)
    }
}
