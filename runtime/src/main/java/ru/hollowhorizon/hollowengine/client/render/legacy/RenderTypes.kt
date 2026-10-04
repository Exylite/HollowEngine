package ru.hollowhorizon.hollowengine.client.render.legacy

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import ru.hollowhorizon.hollowengine.common.registry.ModShaders
import net.minecraft.client.renderer.rendertype.RenderType as VanillaRenderType
/**
 * What the engine says when it wants something drawn the way vanilla draws entities: a texture, and
 * whether it blends and culls. Vanilla's own render types are built from it when the vertices are
 * handed over, see [RecordingBufferSource]; for the engine's own GL drawing [setupRenderState] sets
 * up the same state by hand.
 */
class RenderType private constructor(
    val name: String,
    private val kind: Kind,
    val texture: Identifier?,
    val translucent: Boolean,
    val cull: Boolean,
) {
    private enum class Kind { ENTITY, LINES }

    /** Triangles are what the engine produces; vanilla entity types take quads, see [RecordingBufferSource]. */
    val isLines: Boolean get() = kind == Kind.LINES

    fun vanilla(): VanillaRenderType = when (kind) {
        Kind.LINES -> RenderTypes.lines()
        Kind.ENTITY -> {
            val texture = texture ?: error("An entity render type needs a texture")
            when {
                translucent -> RenderTypes.entityTranslucent(texture)
                cull -> RenderTypes.entityCutoutCull(texture)
                else -> RenderTypes.entityCutout(texture)
            }
        }
    }

    /** The state the engine's shaders expect to find: this type's texture, light and overlay samplers, blend and cull. */
    fun setupRenderState() {
        RenderSystem.setShaderTexture(0, texture?.let(LegacyGl::textureId) ?: 0)
        RenderSystem.setShaderTexture(1, LegacyGl.overlayTextureId())
        RenderSystem.setShaderTexture(2, LegacyGl.lightmapTextureId())
        if (translucent) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
        } else {
            RenderSystem.disableBlend()
        }
        if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(true)
    }

    fun clearRenderState() {
        RenderSystem.disableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableCull()
    }

    /** What [ImmediateBufferSource] assembles this type's geometry as. */
    internal val immediateMode: VertexFormat.Mode
        get() = if (isLines) VertexFormat.Mode.DEBUG_LINES else VertexFormat.Mode.TRIANGLES

    internal val immediateFormat: VertexFormat
        get() = if (isLines) DefaultVertexFormat.POSITION_COLOR else DefaultVertexFormat.NEW_ENTITY

    override fun toString() = name

    companion object {
        @JvmStatic
        fun entityCutoutNoCull(texture: Identifier) = RenderType("entity_cutout_no_cull", Kind.ENTITY, texture, false, false)

        @JvmStatic
        fun entityCutout(texture: Identifier) = RenderType("entity_cutout", Kind.ENTITY, texture, false, true)

        @JvmStatic
        fun entityTranslucent(texture: Identifier) = RenderType("entity_translucent", Kind.ENTITY, texture, true, false)

        @JvmStatic
        fun entity(texture: Identifier, translucent: Boolean, cull: Boolean) =
            RenderType("hollowengine:entity", Kind.ENTITY, texture, translucent, cull)

        @JvmStatic
        fun lines(name: String) = RenderType(name, Kind.LINES, null, true, false)
    }
}

/** The 1.21 `MultiBufferSource`: somewhere to ask for a consumer by render type. */
interface MultiBufferSource {
    fun getBuffer(type: RenderType): VertexConsumer

    interface BufferSource : MultiBufferSource {
        fun endBatch()
        fun endBatch(type: RenderType)
    }
}

/**
 * Draws what the engine writes the moment it is asked to, with the plain programs the engine ships:
 * for the places that already run inside a frame the engine is drawing by hand, such as the UI and
 * the world stages. Only line types are drawn here; entity geometry belongs to the model pipeline.
 */
object ImmediateBufferSource : MultiBufferSource.BufferSource {
    private val builders = LinkedHashMap<RenderType, BufferBuilder>()

    override fun getBuffer(type: RenderType): VertexConsumer =
        builders.getOrPut(type) { BufferBuilder(type.immediateMode, type.immediateFormat) }

    override fun endBatch() {
        val pending = ArrayList(builders.entries)
        builders.clear()
        for ((type, builder) in pending) draw(type, builder)
    }

    override fun endBatch(type: RenderType) {
        builders.remove(type)?.let { draw(type, it) }
    }

    private fun draw(type: RenderType, builder: BufferBuilder) {
        val mesh = builder.build() ?: return
        val shader = if (type.isLines) ModShaders.POSITION_COLOR else null
        if (shader == null) {
            mesh.close()
            return
        }
        LegacyGl.scope {
            type.setupRenderState()
            RenderSystem.setShader(shader)
            BufferUploader.drawWithShader(mesh)
            type.clearRenderState()
        }
    }
}

/** What `Minecraft.renderBuffers()` used to be: the one place to ask for a consumer and have it drawn at once. */
object LegacyRenderBuffers {
    fun bufferSource(): MultiBufferSource.BufferSource = ImmediateBufferSource
}

/**
 * Collects what the engine writes per render type and, when asked, replays it into vanilla's
 * submit collector: vanilla draws entities after they have all been submitted, so nothing can be
 * written straight into a buffer any more.
 *
 * Whatever is written here is taken as already in the space the entity was submitted in, since the
 * engine multiplies it with the pose stack itself.
 */
class RecordingBufferSource(val collector: SubmitNodeCollector) : MultiBufferSource.BufferSource {
    private val recorders = LinkedHashMap<RenderType, Recorder>()

    override fun getBuffer(type: RenderType): VertexConsumer = recorders.getOrPut(type) { Recorder(type) }

    override fun endBatch() {
        for ((type, recorder) in recorders) submit(type, recorder)
        recorders.clear()
    }

    override fun endBatch(type: RenderType) {
        recorders.remove(type)?.let { submit(type, it) }
    }

    private fun submit(type: RenderType, recorder: Recorder) {
        if (recorder.vertexCount == 0) return
        collector.submitCustomGeometry(PoseStack(), type.vanilla()) { _, consumer -> recorder.replay(consumer, type) }
    }

    private class Recorder(val type: RenderType) : VertexConsumer {
        private var data = FloatArray(0)
        private var packed = IntArray(0)
        var vertexCount = 0
            private set

        private var x = 0f
        private var y = 0f
        private var z = 0f
        private var color = -1
        private var u = 0f
        private var v = 0f
        private var overlay = 0
        private var light = 0
        private var nx = 0f
        private var ny = 1f
        private var nz = 0f
        private var open = false

        private fun commit() {
            if (!open) return
            val at = vertexCount * STRIDE
            if (at + STRIDE > data.size) {
                data = data.copyOf(maxOf(256 * STRIDE, data.size * 2))
                packed = packed.copyOf(data.size / STRIDE * PACKED)
            }
            data[at] = x; data[at + 1] = y; data[at + 2] = z
            data[at + 3] = u; data[at + 4] = v
            data[at + 5] = nx; data[at + 6] = ny; data[at + 7] = nz
            val packedAt = vertexCount * PACKED
            packed[packedAt] = color; packed[packedAt + 1] = overlay; packed[packedAt + 2] = light
            vertexCount++
            open = false
        }

        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            commit()
            this.x = x; this.y = y; this.z = z
            color = -1; u = 0f; v = 0f; overlay = 0; light = 0; nx = 0f; ny = 1f; nz = 0f
            open = true
            return this
        }

        override fun setColor(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
            color = (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
            return this
        }

        override fun setColor(color: Int): VertexConsumer {
            this.color = color
            return this
        }

        override fun setUv(u: Float, v: Float): VertexConsumer {
            this.u = u; this.v = v
            return this
        }

        override fun setUv1(u: Int, v: Int): VertexConsumer {
            overlay = (u and 0xFFFF) or (v shl 16)
            return this
        }

        override fun setUv2(u: Int, v: Int): VertexConsumer {
            light = (u and 0xFFFF) or (v shl 16)
            return this
        }

        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer {
            nx = x; ny = y; nz = z
            return this
        }

        override fun setLineWidth(width: Float): VertexConsumer = this

        fun replay(out: VertexConsumer, type: RenderType) {
            commit()
            if (type.isLines) {
                for (i in 0 until vertexCount) put(out, i)
                return
            }
            // vanilla's entity types are quads, and the engine writes triangles: the last corner is written twice
            var i = 0
            while (i + 2 < vertexCount) {
                put(out, i); put(out, i + 1); put(out, i + 2); put(out, i + 2)
                i += 3
            }
        }

        private fun put(out: VertexConsumer, index: Int) {
            val at = index * STRIDE
            val packedAt = index * PACKED
            out.addVertex(data[at], data[at + 1], data[at + 2])
            out.setColor(packed[packedAt])
            out.setUv(data[at + 3], data[at + 4])
            val overlay = packed[packedAt + 1]
            out.setUv1(overlay and 0xFFFF, overlay ushr 16)
            val light = packed[packedAt + 2]
            out.setUv2(light and 0xFFFF, light ushr 16)
            out.setNormal(data[at + 5], data[at + 6], data[at + 7])
        }

        companion object {
            private const val STRIDE = 8
            private const val PACKED = 3
        }
    }
}
