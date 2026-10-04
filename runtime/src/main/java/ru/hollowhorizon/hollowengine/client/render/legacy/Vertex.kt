package ru.hollowhorizon.hollowengine.client.render.legacy

import com.mojang.blaze3d.vertex.VertexConsumer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One attribute of a [VertexFormat]. The attribute location is the element's position in the
 * format, which is where [ShaderInstance] binds the shader attribute of the same name.
 */
class VertexFormatElement(
    val name: String,
    val glType: Int,
    val count: Int,
    val byteSize: Int,
    val normalized: Boolean,
    val integer: Boolean,
    val padding: Boolean = false,
) {
    internal fun setup(index: Int, offset: Long, stride: Int) {
        if (padding) return
        GL20.glEnableVertexAttribArray(index)
        if (integer) GL30.glVertexAttribIPointer(index, count, glType, stride, offset)
        else GL20.glVertexAttribPointer(index, count, glType, normalized, stride, offset)
    }

    companion object {
        val POSITION = VertexFormatElement("Position", GL11.GL_FLOAT, 3, 12, false, false)
        val COLOR = VertexFormatElement("Color", GL11.GL_UNSIGNED_BYTE, 4, 4, true, false)
        val UV0 = VertexFormatElement("UV0", GL11.GL_FLOAT, 2, 8, false, false)
        val UV1 = VertexFormatElement("UV1", GL11.GL_SHORT, 2, 4, false, true)
        val UV2 = VertexFormatElement("UV2", GL11.GL_SHORT, 2, 4, false, true)
        val NORMAL = VertexFormatElement("Normal", GL11.GL_BYTE, 3, 3, true, false)
        val PADDING = VertexFormatElement("Padding", GL11.GL_BYTE, 1, 1, false, false, padding = true)
    }
}

class VertexFormat(val elements: List<VertexFormatElement>) {
    val offsets: IntArray
    val vertexSize: Int

    init {
        var offset = 0
        offsets = IntArray(elements.size) { i ->
            val current = offset
            offset += elements[i].byteSize
            current
        }
        vertexSize = offset
    }

    val elementAttributeNames: List<String> get() = elements.map { it.name }

    fun offsetOf(name: String): Int = offsets[elements.indexOfFirst { it.name == name }]

    fun contains(name: String): Boolean = elements.any { it.name == name }

    /** Points the attributes at the array buffer that is bound right now. */
    fun setupBufferState() {
        for (i in elements.indices) elements[i].setup(i, offsets[i].toLong(), vertexSize)
    }

    fun clearBufferState() {
        for (i in elements.indices) if (!elements[i].padding) GL20.glDisableVertexAttribArray(i)
    }

    /** The one buffer [BufferUploader] uploads and draws this format's meshes through. */
    val immediateDrawVertexBuffer: VertexBuffer by lazy(LazyThreadSafetyMode.NONE) { VertexBuffer(VertexBuffer.Usage.DYNAMIC) }

    override fun toString(): String = "VertexFormat" + elements.joinToString(", ", "[", "]") { it.name }

    enum class Mode(val glMode: Int, val primitiveLength: Int, val primitiveStride: Int, val connectedPrimitives: Boolean) {
        LINES(GL11.GL_LINES, 2, 2, false),
        LINE_STRIP(GL11.GL_LINE_STRIP, 2, 1, true),
        DEBUG_LINES(GL11.GL_LINES, 2, 2, false),
        DEBUG_LINE_STRIP(GL11.GL_LINE_STRIP, 2, 1, true),
        TRIANGLES(GL11.GL_TRIANGLES, 3, 3, false),
        TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP, 3, 1, true),
        TRIANGLE_FAN(GL11.GL_TRIANGLE_FAN, 3, 1, true),
        QUADS(GL11.GL_TRIANGLES, 4, 4, false);

        /** Core profile has no quads: they are drawn as two indexed triangles each. */
        val indexed: Boolean get() = this == QUADS

        fun indexCount(vertexCount: Int): Int = if (this == QUADS) vertexCount / 4 * 6 else vertexCount
    }
}

object DefaultVertexFormat {
    val POSITION = VertexFormat(listOf(VertexFormatElement.POSITION))
    val POSITION_COLOR = VertexFormat(listOf(VertexFormatElement.POSITION, VertexFormatElement.COLOR))
    val POSITION_TEX = VertexFormat(listOf(VertexFormatElement.POSITION, VertexFormatElement.UV0))
    val POSITION_TEX_COLOR =
        VertexFormat(listOf(VertexFormatElement.POSITION, VertexFormatElement.UV0, VertexFormatElement.COLOR))
    val POSITION_COLOR_NORMAL = VertexFormat(
        listOf(
            VertexFormatElement.POSITION, VertexFormatElement.COLOR, VertexFormatElement.NORMAL,
            VertexFormatElement.PADDING
        )
    )
    val PARTICLE = VertexFormat(
        listOf(VertexFormatElement.POSITION, VertexFormatElement.UV0, VertexFormatElement.COLOR, VertexFormatElement.UV2)
    )
    val NEW_ENTITY = VertexFormat(
        listOf(
            VertexFormatElement.POSITION, VertexFormatElement.COLOR, VertexFormatElement.UV0,
            VertexFormatElement.UV1, VertexFormatElement.UV2, VertexFormatElement.NORMAL, VertexFormatElement.PADDING
        )
    )
}

/** Only here so the calls that hand a sorting to the render system keep compiling; nothing sorts. */
enum class VertexSorting { ORTHOGRAPHIC_Z, DISTANCE_TO_ORIGIN }

/** A finished vertex buffer, waiting to be drawn or uploaded. */
class MeshData internal constructor(
    internal var buffer: ByteBuffer?,
    private val vertexCount: Int,
    private val format: VertexFormat,
    private val mode: VertexFormat.Mode,
) : AutoCloseable {
    class DrawState(val format: VertexFormat, val vertexCount: Int, val mode: VertexFormat.Mode) {
        fun format() = format
        fun vertexCount() = vertexCount
        fun mode() = mode
        fun indexCount() = mode.indexCount(vertexCount)
    }

    private val state = DrawState(format, vertexCount, mode)

    fun drawState(): DrawState = state

    fun vertexBuffer(): ByteBuffer = buffer ?: error("The mesh was closed")

    override fun close() {
        buffer = null
    }
}

/**
 * Fills a direct buffer in the layout of a [VertexFormat]; the 1.21 BufferBuilder, minus the pooled
 * memory: the buffer simply belongs to the builder and goes with it.
 */
class BufferBuilder(private val mode: VertexFormat.Mode, private val format: VertexFormat, initialVertices: Int = 256) :
    VertexConsumer {
    private var buffer: ByteBuffer = allocate(initialVertices * format.vertexSize)
    private var vertexCount = 0
    private var vertexStart = -1
    private var built = false

    private fun allocate(size: Int): ByteBuffer =
        ByteBuffer.allocateDirect(maxOf(size, format.vertexSize * 4)).order(ByteOrder.nativeOrder())

    override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
        check(!built) { "The builder was already built" }
        endVertex()
        vertexStart = buffer.position()
        if (buffer.remaining() < format.vertexSize) {
            val bigger = allocate(buffer.capacity() * 2)
            buffer.flip()
            bigger.put(buffer)
            buffer = bigger
        }
        // a vertex that leaves out an element is zero in it, not whatever the buffer held before
        MemoryUtil.memSet(MemoryUtil.memAddress(buffer, vertexStart), 0, format.vertexSize.toLong())
        buffer.position(vertexStart + format.vertexSize)
        vertexCount++
        buffer.putFloat(vertexStart, x)
        buffer.putFloat(vertexStart + 4, y)
        buffer.putFloat(vertexStart + 8, z)
        return this
    }

    private fun endVertex() {}

    private fun at(name: String): Int {
        val offset = format.offsetOf(name)
        return vertexStart + offset
    }

    private fun has(name: String) = format.contains(name)

    override fun setColor(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
        if (!has("Color")) return this
        val at = at("Color")
        buffer.put(at, r.toByte())
        buffer.put(at + 1, g.toByte())
        buffer.put(at + 2, b.toByte())
        buffer.put(at + 3, a.toByte())
        return this
    }

    /** ARGB, as everywhere in vanilla. */
    override fun setColor(color: Int): VertexConsumer =
        setColor(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, color ushr 24)

    override fun setUv(u: Float, v: Float): VertexConsumer {
        if (!has("UV0")) return this
        val at = at("UV0")
        buffer.putFloat(at, u)
        buffer.putFloat(at + 4, v)
        return this
    }

    override fun setUv1(u: Int, v: Int): VertexConsumer {
        if (!has("UV1")) return this
        val at = at("UV1")
        buffer.putShort(at, u.toShort())
        buffer.putShort(at + 2, v.toShort())
        return this
    }

    override fun setUv2(u: Int, v: Int): VertexConsumer {
        if (!has("UV2")) return this
        val at = at("UV2")
        buffer.putShort(at, u.toShort())
        buffer.putShort(at + 2, v.toShort())
        return this
    }

    override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer {
        if (!has("Normal")) return this
        val at = at("Normal")
        buffer.put(at, (x.coerceIn(-1f, 1f) * 127f).toInt().toByte())
        buffer.put(at + 1, (y.coerceIn(-1f, 1f) * 127f).toInt().toByte())
        buffer.put(at + 2, (z.coerceIn(-1f, 1f) * 127f).toInt().toByte())
        return this
    }

    override fun setLineWidth(width: Float): VertexConsumer = this

    fun build(): MeshData? {
        check(!built) { "The builder was already built" }
        built = true
        if (vertexCount == 0) return null
        val used = buffer
        used.position(0)
        used.limit(vertexCount * format.vertexSize)
        return MeshData(used, vertexCount, format, mode)
    }

    fun buildOrThrow(): MeshData = build() ?: throw IllegalStateException("BufferBuilder was empty")
}

class Tesselator private constructor() {
    fun begin(mode: VertexFormat.Mode, format: VertexFormat): BufferBuilder = BufferBuilder(mode, format)

    companion object {
        private val instance = Tesselator()

        @JvmStatic
        fun getInstance(): Tesselator = instance
    }
}

/** A VAO with its vertex buffer, in the shape of the 1.21 class of the same name. */
class VertexBuffer(private val usage: Usage = Usage.STATIC) : AutoCloseable {
    enum class Usage(val glUsage: Int) { STATIC(GL15.GL_STATIC_DRAW), DYNAMIC(GL15.GL_DYNAMIC_DRAW) }

    private var vao = GL30.glGenVertexArrays()
    private var vbo = GL15.glGenBuffers()
    private var format: VertexFormat? = null
    private var mode = VertexFormat.Mode.TRIANGLES
    private var indexCount = 0
    private var vertexCount = 0

    fun bind() {
        GL30.glBindVertexArray(vao)
        boundArray = vao
    }

    fun upload(mesh: MeshData) {
        val meshFormat = mesh.drawState().format
        val state = mesh.drawState()
        val data = mesh.vertexBuffer()
        GL30.glBindVertexArray(vao)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage.glUsage)
        if (format !== meshFormat) {
            format?.clearBufferState()
            meshFormat.setupBufferState()
            format = meshFormat
        }
        mode = state.mode
        vertexCount = state.vertexCount
        indexCount = state.indexCount()
        if (mode.indexed || mode == VertexFormat.Mode.TRIANGLE_FAN) {
            SharedIndices.bind(mode, vertexCount)
        } else {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
        }
        mesh.close()
    }

    fun draw() {
        if (mode.indexed || mode == VertexFormat.Mode.TRIANGLE_FAN) {
            GL11.glDrawElements(mode.glMode, indexCount, GL11.GL_UNSIGNED_INT, 0L)
        } else {
            GL11.glDrawArrays(mode.glMode, 0, vertexCount)
        }
    }

    /** Draws with [shader] and the given matrices, which is what the old `drawWithShader` did. */
    fun drawWithShader(modelView: org.joml.Matrix4f, projection: org.joml.Matrix4f, shader: ShaderInstance) {
        shader.setDefaultUniforms(mode, modelView, projection)
        shader.apply()
        draw()
        shader.clear()
    }

    override fun close() {
        if (vao != 0) {
            GL30.glDeleteVertexArrays(vao)
            GL15.glDeleteBuffers(vbo)
            vao = 0
            vbo = 0
        }
    }

    companion object {
        private var boundArray = 0

        @JvmStatic
        fun unbind() {
            GL30.glBindVertexArray(0)
            boundArray = 0
        }
    }
}

/** The index buffer quads and fans are drawn with: indices for as many vertices as ever asked for. */
internal object SharedIndices {
    private var quadBuffer = 0
    private var quadCapacity = 0
    private var fanBuffer = 0
    private var fanCapacity = 0

    fun bind(mode: VertexFormat.Mode, vertexCount: Int) {
        if (mode == VertexFormat.Mode.QUADS) {
            val quads = vertexCount / 4
            if (quadBuffer == 0) quadBuffer = GL15.glGenBuffers()
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, quadBuffer)
            if (quads > quadCapacity) {
                quadCapacity = maxOf(quads, quadCapacity * 2, 256)
                val data = MemoryUtil.memAllocInt(quadCapacity * 6)
                for (q in 0 until quadCapacity) {
                    val v = q * 4
                    data.put(v).put(v + 1).put(v + 2).put(v + 2).put(v + 3).put(v)
                }
                data.flip()
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW)
                MemoryUtil.memFree(data)
            }
        } else {
            val triangles = maxOf(vertexCount - 2, 0)
            if (fanBuffer == 0) fanBuffer = GL15.glGenBuffers()
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, fanBuffer)
            if (triangles > fanCapacity) {
                fanCapacity = maxOf(triangles, fanCapacity * 2, 256)
                val data = MemoryUtil.memAllocInt(fanCapacity * 3)
                for (t in 0 until fanCapacity) data.put(0).put(t + 1).put(t + 2)
                data.flip()
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW)
                MemoryUtil.memFree(data)
            }
        }
    }
}

object BufferUploader {
    /** Draws [mesh] with the shader the render system holds, then forgets it. */
    @JvmStatic
    fun drawWithShader(mesh: MeshData) {
        val shader = RenderSystem.getShader()
        if (shader == null) {
            mesh.close()
            return
        }
        LegacyGl.scope {
            val format = mesh.drawState().format
            val buffer = format.immediateDrawVertexBuffer
            buffer.bind()
            buffer.upload(mesh)
            buffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader)
            VertexBuffer.unbind()
        }
    }
}
