package ru.hollowhorizon.hollowengine.client.ui.render

import ru.hollowhorizon.hollowengine.client.render.legacy.RenderSystem
import ru.hollowhorizon.hollowengine.client.render.legacy.BufferUploader
import ru.hollowhorizon.hollowengine.client.render.legacy.DefaultVertexFormat
import ru.hollowhorizon.hollowengine.client.render.legacy.Tesselator
import ru.hollowhorizon.hollowengine.client.render.legacy.VertexFormat
import ru.hollowhorizon.hollowengine.common.registry.ModShaders

/** Alpha-only filtering. RGB never participates in a shadow, including RGB hidden by zero alpha. */
internal object UiImageShadowFilter {
    fun draw(source: UiFramebuffer, target: UiFramebuffer, radius: Float, horizontal: Boolean, spread: Boolean) {
        val shader = checkNotNull(ModShaders.UI_IMAGE_SHADOW) { "Image shadow shader has not been loaded" }
        target.bind()
        RenderSystem.setShader { shader }
        RenderSystem.setShaderTexture(0, source.texture)
        shader.getUniform("TexelDirection")?.set(
            if (horizontal) 1f / source.width else 0f,
            if (horizontal) 0f else 1f / source.height,
        )
        shader.getUniform("Radius")?.set(radius)
        shader.getUniform("Spread")?.set(if (spread) 1f else 0f)
        // Writing, not compositing: blending here would square the mask's alpha.
        RenderSystem.disableBlend()
        val buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
        buffer.addVertex(-1f, -1f, 0f).setUv(0f, 0f).setColor(-1)
        buffer.addVertex(1f, -1f, 0f).setUv(1f, 0f).setColor(-1)
        buffer.addVertex(1f, 1f, 0f).setUv(1f, 1f).setColor(-1)
        buffer.addVertex(-1f, 1f, 0f).setUv(0f, 1f).setColor(-1)
        BufferUploader.drawWithShader(buffer.buildOrThrow())
    }
}
