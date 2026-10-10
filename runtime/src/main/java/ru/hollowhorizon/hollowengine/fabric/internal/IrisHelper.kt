package ru.hollowhorizon.hollowengine.fabric.internal

import org.joml.Matrix4f
import ru.hollowhorizon.hollowengine.client.models.internal.rendering.ModelInstancingBackend
import ru.hollowhorizon.hollowengine.client.models.internal.rendering.PipelineRenderer
import ru.hollowhorizon.hollowengine.client.models.internal.rendering.VanillaInstancingBackend
import ru.hollowhorizon.hollowengine.client.render.legacy.RenderSystem
import ru.hollowhorizon.hollowengine.client.utils.InstancingEntityInfo

/**
 * The seam to Iris. There is no Iris for 26.2 that the engine can compile against and run beside
 * its own GL drawing yet, so the shader pack is never in charge: the engine draws with its own
 * programs and the vanilla instancing backend. Everything that used to ask Iris asks here instead,
 * so the integration can come back in one place.
 */
object IrisHelper {
    val hasIris = false

    @JvmStatic
    fun shouldOverrideShaders() = false

    fun areShadersEnabled() = false

    fun isShadowRendering() = false

    fun isShaderPackInUse() = false

    fun currentGbufferModelViewMatrix(): Matrix4f = Matrix4f(RenderSystem.getModelViewMatrix())

    fun currentGbufferProjectionMatrix(fallback: Matrix4f): Matrix4f = Matrix4f(fallback)

    fun instancingBackend(): ModelInstancingBackend = VanillaInstancingBackend

    fun capturedEntityInfo(): InstancingEntityInfo = InstancingEntityInfo()

    @JvmStatic
    fun invalidateInstancingPrograms() {
        PipelineRenderer.invalidateRuntimeInstancedBindings()
    }
}
