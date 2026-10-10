package ru.hollowhorizon.hollowengine.client.render.legacy

import com.google.gson.JsonParser
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceProvider
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL33
import java.io.IOException
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * A uniform of a [ShaderInstance]: values are kept here and sent to the program by [upload], like
 * the 1.21 class of the same name.
 */
class Uniform(val name: String, val type: Int, val count: Int, private val parent: ShaderInstance?) : AutoCloseable {
    var location: Int = -1
    private val ints: IntBuffer? = if (type <= UT_INT4) BufferUtils.createIntBuffer(count) else null
    private val floats: FloatBuffer? = if (type > UT_INT4) BufferUtils.createFloatBuffer(count) else null
    private var dirty = true

    private fun markDirty() {
        dirty = true
        parent?.markDirty()
    }

    fun set(x: Float) {
        floats!!.position(0); floats.put(0, x); markDirty()
    }

    fun set(x: Float, y: Float) {
        floats!!.position(0); floats.put(0, x); floats.put(1, y); markDirty()
    }

    fun set(x: Float, y: Float, z: Float) {
        floats!!.position(0); floats.put(0, x); floats.put(1, y); floats.put(2, z); markDirty()
    }

    fun set(x: Float, y: Float, z: Float, w: Float) {
        floats!!.position(0); floats.put(0, x); floats.put(1, y); floats.put(2, z); floats.put(3, w); markDirty()
    }

    fun set(v: Vector3f) = set(v.x, v.y, v.z)
    fun set(v: Vector4f) = set(v.x, v.y, v.z, v.w)

    fun set(values: FloatArray) {
        val buffer = floats!!
        if (values.size < count) return
        buffer.position(0)
        buffer.put(values, 0, count)
        buffer.position(0)
        markDirty()
    }

    fun set(matrix: Matrix4f) {
        val buffer = floats!!
        buffer.position(0)
        matrix.get(buffer)
        markDirty()
    }

    fun set(matrix: Matrix3f) {
        val buffer = floats!!
        buffer.position(0)
        matrix.get(buffer)
        markDirty()
    }

    fun set(x: Int) {
        if (ints != null) {
            ints.position(0); ints.put(0, x)
        } else {
            floats!!.position(0); floats.put(0, x.toFloat())
        }
        markDirty()
    }

    fun set(x: Int, y: Int) {
        ints!!.position(0); ints.put(0, x); ints.put(1, y); markDirty()
    }

    fun set(x: Int, y: Int, z: Int) {
        ints!!.position(0); ints.put(0, x); ints.put(1, y); ints.put(2, z); markDirty()
    }

    fun set(x: Int, y: Int, z: Int, w: Int) {
        ints!!.position(0); ints.put(0, x); ints.put(1, y); ints.put(2, z); ints.put(3, w); markDirty()
    }

    /** Writes as many of the values as the uniform has components, and ignores the rest. */
    fun setSafe(x: Float, y: Float, z: Float, w: Float) {
        val buffer = floats ?: return
        buffer.position(0)
        if (type >= UT_FLOAT1) buffer.put(0, x)
        if (type >= UT_FLOAT2) buffer.put(1, y)
        if (type >= UT_FLOAT3) buffer.put(2, z)
        if (type >= UT_FLOAT4) buffer.put(3, w)
        markDirty()
    }

    fun setSafe(x: Int, y: Int, z: Int, w: Int) {
        val buffer = ints ?: return
        buffer.position(0)
        if (type >= UT_INT1) buffer.put(0, x)
        if (type >= UT_INT2) buffer.put(1, y)
        if (type >= UT_INT3) buffer.put(2, z)
        if (type >= UT_INT4) buffer.put(3, w)
        markDirty()
    }

    fun upload() {
        if (!dirty && !ALWAYS_UPLOAD) return
        dirty = false
        if (location == -1) return
        when (type) {
            UT_INT1 -> GL20.glUniform1iv(location, ints!!)
            UT_INT2 -> GL20.glUniform2iv(location, ints!!)
            UT_INT3 -> GL20.glUniform3iv(location, ints!!)
            UT_INT4 -> GL20.glUniform4iv(location, ints!!)
            UT_FLOAT1 -> GL20.glUniform1fv(location, floats!!)
            UT_FLOAT2 -> GL20.glUniform2fv(location, floats!!)
            UT_FLOAT3 -> GL20.glUniform3fv(location, floats!!)
            UT_FLOAT4 -> GL20.glUniform4fv(location, floats!!)
            UT_MAT2 -> GL20.glUniformMatrix2fv(location, false, floats!!)
            UT_MAT3 -> GL20.glUniformMatrix3fv(location, false, floats!!)
            UT_MAT4 -> GL20.glUniformMatrix4fv(location, false, floats!!)
        }
    }

    override fun close() {}

    companion object {
        const val UT_INT1 = 0
        const val UT_INT2 = 1
        const val UT_INT3 = 2
        const val UT_INT4 = 3
        const val UT_FLOAT1 = 4
        const val UT_FLOAT2 = 5
        const val UT_FLOAT3 = 6
        const val UT_FLOAT4 = 7
        const val UT_MAT2 = 8
        const val UT_MAT3 = 9
        const val UT_MAT4 = 10

        // programs are shared between draws: a uniform another draw changed must be sent again
        private const val ALWAYS_UPLOAD = true

        fun typeOf(name: String, count: Int): Int = when (name) {
            "int" -> UT_INT1 + count - 1
            "float" -> UT_FLOAT1 + count - 1
            "matrix2x2" -> UT_MAT2
            "matrix3x3" -> UT_MAT3
            "matrix4x4" -> UT_MAT4
            else -> throw IOException("Unknown uniform type $name")
        }
    }
}

/**
 * A GL program described by the 1.21 core shader JSON: `shaders/core/<name>.json` names the vertex
 * and fragment sources, the samplers and the uniforms with their defaults. The `blend` of the JSON
 * is not applied: the engine sets its blending itself, before it applies a shader. `#moj_import` lines are
 * expanded here, from the engine's own copies of the old vanilla includes first, because vanilla's
 * include files no longer match the shaders the engine ships.
 */
class ShaderInstance(provider: ResourceProvider, val name: String, val vertexFormat: VertexFormat) : AutoCloseable {
    val id: Int
    private val uniforms = ArrayList<Uniform>()
    private val uniformMap = HashMap<String, Uniform>()
    private val samplerNames = ArrayList<String>()
    val samplerLocations: IntArray
    private val samplerMap = HashMap<String, Any>()
    private val vertexShader: Int
    private val fragmentShader: Int

    @JvmField val MODEL_VIEW_MATRIX: Uniform?
    @JvmField val PROJECTION_MATRIX: Uniform?
    @JvmField val COLOR_MODULATOR: Uniform?
    @JvmField val LIGHT0_DIRECTION: Uniform?
    @JvmField val LIGHT1_DIRECTION: Uniform?
    @JvmField val FOG_START: Uniform?
    @JvmField val FOG_END: Uniform?
    @JvmField val FOG_COLOR: Uniform?
    @JvmField val FOG_SHAPE: Uniform?
    @JvmField val GAME_TIME: Uniform?
    @JvmField val SCREEN_SIZE: Uniform?
    @JvmField val LINE_WIDTH: Uniform?
    @JvmField val TEXTURE_MATRIX: Uniform?

    init {
        val location = Identifier.parse(name)
        val jsonLocation = location.withPath("shaders/core/${location.path}.json")
        val json = try {
            provider.getResourceOrThrow(jsonLocation).openAsReader().use { JsonParser.parseReader(it).asJsonObject }
        } catch (e: Exception) {
            throw IOException("Could not load shader program $jsonLocation: ${e.message}", e)
        }

        val vertexName = Identifier.parse(json.get("vertex").asString)
        val fragmentName = Identifier.parse(json.get("fragment").asString)

        vertexShader = compile(provider, GL20.GL_VERTEX_SHADER, vertexName.withPath("shaders/core/${vertexName.path}.vsh"))
        fragmentShader =
            compile(provider, GL20.GL_FRAGMENT_SHADER, fragmentName.withPath("shaders/core/${fragmentName.path}.fsh"))

        id = GL20.glCreateProgram()
        GL20.glAttachShader(id, vertexShader)
        GL20.glAttachShader(id, fragmentShader)
        vertexFormat.elements.forEachIndexed { index, element ->
            if (!element.padding) GL20.glBindAttribLocation(id, index, element.name)
        }
        GL20.glLinkProgram(id)
        if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            val log = GL20.glGetProgramInfoLog(id, 32768)
            close()
            throw IOException("Failed to link shader program $name: $log")
        }

        json.getAsJsonArray("samplers")?.forEach { samplerNames.add(it.asJsonObject.get("name").asString) }
        samplerLocations = IntArray(samplerNames.size) { GL20.glGetUniformLocation(id, samplerNames[it]) }

        json.getAsJsonArray("uniforms")?.forEach { element ->
            val obj = element.asJsonObject
            val uniformName = obj.get("name").asString
            val count = obj.get("count").asInt
            val uniform = Uniform(uniformName, Uniform.typeOf(obj.get("type").asString, count), count, this)
            uniform.location = GL20.glGetUniformLocation(id, uniformName)
            val values = obj.getAsJsonArray("values")
            if (values != null && values.size() >= count) {
                val array = FloatArray(count) { values[it].asFloat }
                if (uniform.type <= Uniform.UT_INT4) {
                    when (count) {
                        1 -> uniform.set(array[0].toInt())
                        2 -> uniform.set(array[0].toInt(), array[1].toInt())
                        3 -> uniform.set(array[0].toInt(), array[1].toInt(), array[2].toInt())
                        else -> uniform.set(array[0].toInt(), array[1].toInt(), array[2].toInt(), array[3].toInt())
                    }
                } else uniform.set(array)
            }
            uniforms.add(uniform)
            uniformMap[uniformName] = uniform
        }

        MODEL_VIEW_MATRIX = uniformMap["ModelViewMat"]
        PROJECTION_MATRIX = uniformMap["ProjMat"]
        COLOR_MODULATOR = uniformMap["ColorModulator"]
        LIGHT0_DIRECTION = uniformMap["Light0_Direction"]
        LIGHT1_DIRECTION = uniformMap["Light1_Direction"]
        FOG_START = uniformMap["FogStart"]
        FOG_END = uniformMap["FogEnd"]
        FOG_COLOR = uniformMap["FogColor"]
        FOG_SHAPE = uniformMap["FogShape"]
        GAME_TIME = uniformMap["GameTime"]
        SCREEN_SIZE = uniformMap["ScreenSize"]
        LINE_WIDTH = uniformMap["LineWidth"]
        TEXTURE_MATRIX = uniformMap["TextureMat"]
    }

    internal fun markDirty() {}

    fun getUniform(name: String): Uniform? = uniformMap[name]

    /** The uniform, or one that goes nowhere when the program does not have it. */
    fun safeGetUniform(name: String): Uniform = uniformMap[name] ?: DUMMY_UNIFORM

    /** [value] is a GL texture name, or an [AbstractTexture] or [RenderTarget] to take the name from. */
    fun setSampler(name: String, value: Any) {
        samplerMap[name] = value
    }

    fun setDefaultUniforms(mode: VertexFormat.Mode, modelView: Matrix4f, projection: Matrix4f) {
        for (i in 0 until 12) setSampler("Sampler$i", RenderSystem.getShaderTexture(i))
        MODEL_VIEW_MATRIX?.set(modelView)
        PROJECTION_MATRIX?.set(projection)
        COLOR_MODULATOR?.set(RenderSystem.getShaderColor())
        FOG_START?.set(RenderSystem.fogStart)
        FOG_END?.set(RenderSystem.fogEnd)
        FOG_COLOR?.set(RenderSystem.fogColor)
        FOG_SHAPE?.set(RenderSystem.fogShape)
        TEXTURE_MATRIX?.set(Matrix4f())
        GAME_TIME?.set(RenderSystem.gameTime)
        SCREEN_SIZE?.set(RenderSystem.screenWidth.toFloat(), RenderSystem.screenHeight.toFloat())
        if (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP) LINE_WIDTH?.set(RenderSystem.lineWidth)
        LIGHT0_DIRECTION?.set(RenderSystem.light0)
        LIGHT1_DIRECTION?.set(RenderSystem.light1)
    }

    fun setDefaultUniforms(mode: VertexFormat.Mode, modelView: Matrix4f, projection: Matrix4f, window: Any?) =
        setDefaultUniforms(mode, modelView, projection)

    /** Makes this the program in use, with its samplers and uniforms. */
    fun apply() {
        GL20.glUseProgram(id)
        val previous = LegacyGl.activeTextureUnit()
        for (i in samplerNames.indices) {
            val sampler = samplerMap[samplerNames[i]] ?: continue
            val location = samplerLocations[i]
            if (location != -1) GL20.glUniform1i(location, i)
            val texture = when (sampler) {
                is Int -> sampler
                is AbstractTexture -> LegacyGl.textureId(sampler)
                is RenderTarget -> sampler.colorTextureId
                else -> -1
            }
            if (texture != -1) LegacyGl.bindTexture(i, texture)
        }
        com.mojang.blaze3d.opengl.GlStateManager._activeTexture(GL33.GL_TEXTURE0 + previous)
        for (uniform in uniforms) uniform.upload()
    }

    fun clear() {
        GL20.glUseProgram(0)
    }

    override fun close() {
        if (id != 0) GL20.glDeleteProgram(id)
        if (vertexShader != 0) GL20.glDeleteShader(vertexShader)
        if (fragmentShader != 0) GL20.glDeleteShader(fragmentShader)
        uniforms.forEach(Uniform::close)
    }

    private fun compile(provider: ResourceProvider, type: Int, location: Identifier): Int {
        val source = Preprocessor(provider).process(location)
        val shader = GL20.glCreateShader(type)
        GL20.glShaderSource(shader, source)
        GL20.glCompileShader(shader)
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            val log = GL20.glGetShaderInfoLog(shader, 32768)
            GL20.glDeleteShader(shader)
            throw IOException("Couldn't compile $location: $log")
        }
        return shader
    }

    /** Expands `#moj_import <file>` the way the old vanilla preprocessor did, once per file. */
    private class Preprocessor(private val provider: ResourceProvider) {
        private val imported = HashSet<Identifier>()

        fun process(location: Identifier): String {
            val text = read(location)
            return expand(text)
        }

        private fun read(location: Identifier): String =
            try {
                provider.getResourceOrThrow(location).openAsReader().use { it.readText() }
            } catch (e: Exception) {
                throw IOException("Could not read shader source $location: ${e.message}", e)
            }

        private fun expand(text: String): String {
            val out = StringBuilder()
            for (line in text.lines()) {
                val trimmed = line.trim()
                if (!trimmed.startsWith("#moj_import")) {
                    out.append(line).append('\n')
                    continue
                }
                val open = trimmed.indexOf('<')
                val close = trimmed.indexOf('>')
                val wanted = if (open >= 0 && close > open) trimmed.substring(open + 1, close)
                else trimmed.substringAfter('"').substringBefore('"')
                val resolved = resolve(wanted)
                if (imported.add(resolved)) out.append(expand(read(resolved))).append('\n')
            }
            return out.toString()
        }

        private fun resolve(wanted: String): Identifier {
            val explicit = Identifier.tryParse(wanted)?.takeIf { wanted.contains(':') }
            if (explicit != null) return explicit.withPath("shaders/include/${explicit.path}")
            val candidates = listOf(
                Identifier.fromNamespaceAndPath("hollowengine", "shaders/include/legacy/$wanted"),
                Identifier.fromNamespaceAndPath("hollowengine", "shaders/include/$wanted"),
                Identifier.fromNamespaceAndPath("minecraft", "shaders/include/$wanted"),
            )
            return candidates.firstOrNull { provider.getResource(it).isPresent } ?: candidates.last()
        }
    }

    companion object {
        // takes whatever is written to a uniform the program does not have, up to a matrix
        val DUMMY_UNIFORM = Uniform("dummy", Uniform.UT_FLOAT1, 16, null)
    }
}
