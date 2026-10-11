package ru.hollowhorizon.hollowengine.client.models.internal

import ru.hollowhorizon.hollowengine.client.render.legacy.RenderSystem
import ru.hollowhorizon.hollowengine.client.render.legacy.DefaultVertexFormat
import ru.hollowhorizon.hollowengine.client.render.legacy.ShaderInstance
import ru.hollowhorizon.hollowengine.client.render.legacy.VertexFormat
import net.minecraft.util.Util
import net.minecraft.util.LightCoordsUtil
import ru.hollowhorizon.hollowengine.client.render.legacy.RenderType
import org.lwjgl.opengl.GL13
import ru.hollowhorizon.hollowengine.common.registry.ModShaders
import java.util.function.Function


inline fun drawWithShader(
    shader: ShaderInstance = SHADER,
    state: RenderType = translucentShaderState(),
    body: () -> Unit,
) {
    state.setupRenderState()
    RenderSystem.setShader(shader)
    shader.setDefaultUniforms(
        VertexFormat.Mode.TRIANGLES,
        RenderSystem.getModelViewMatrix(),
        RenderSystem.getProjectionMatrix(),
    )
    shader.apply()

    shader.samplerLocations.forEachIndexed { texture, index ->
        if (index != -1) RenderSystem.glUniform1i(index, texture)
    }

    body()

    shader.clear()
    state.clearRenderState()
}

fun Material.packedLight(worldLight: Int): Int = if (emissive) LightCoordsUtil.FULL_BRIGHT else worldLight

fun opaqueShaderState(): RenderType = RenderType.entityCutoutNoCull(Material.MISSING_TEXTURE)

fun translucentShaderState(): RenderType = RenderType.entityTranslucent(Material.MISSING_TEXTURE)

/** What the batched path draws a material with: its texture, blending and culling. */
val batchingRenderType: Function<Material, RenderType> = Util.memoize<Material, RenderType> { material: Material ->
    RenderType.entity(material.texture, material.blend == Material.Blend.BLEND, !material.doubleSided)
}

val SHADER
    get() = ModShaders.GLTF_ENTITY // Ванильный шейдер не поддерживает матрицу нормалей

val INSTANCED_SHADER
    get() = ModShaders.GLTF_ENTITY_INSTANCED

const val COLOR_MAP_INDEX = GL13.GL_TEXTURE0
