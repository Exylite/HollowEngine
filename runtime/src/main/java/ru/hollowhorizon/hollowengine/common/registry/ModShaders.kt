package ru.hollowhorizon.hollowengine.common.registry

import ru.hollowhorizon.hollowengine.client.render.legacy.DefaultVertexFormat
import ru.hollowhorizon.hollowengine.client.render.legacy.ShaderInstance
import ru.hollowhorizon.hollowengine.HollowEngine.MODID
import ru.hollowhorizon.hollowengine.client.vfx.render.VfxMeshRenderer
import ru.hollowhorizon.hollowengine.client.vfx.render.VfxQuadRenderer
import ru.hollowhorizon.hollowengine.common.events.ClientOnly
import ru.hollowhorizon.hollowengine.common.events.SubscribeEvent
import ru.hollowhorizon.hollowengine.common.events.registry.RegisterShadersEvent
import ru.hollowhorizon.hollowengine.common.utils.rl

@ClientOnly
object ModShaders {
    lateinit var GLTF_ENTITY: ShaderInstance
    lateinit var GLTF_ENTITY_INSTANCED: ShaderInstance

    /** The entity program, or none while the shaders are not registered yet. */
    val gltfEntityOrNull: ShaderInstance? get() = if (::GLTF_ENTITY.isInitialized) GLTF_ENTITY else null
    var VFX_PARTICLE: ShaderInstance? = null
    var VFX_MESH: ShaderInstance? = null
    var VFX_RIBBON: ShaderInstance? = null
    var UI_EFFECT: ShaderInstance? = null

    // what vanilla's GameRenderer used to hold: the plain programs the engine draws its own geometry with
    var POSITION_COLOR: ShaderInstance? = null
    var POSITION_TEX: ShaderInstance? = null
    var POSITION_TEX_COLOR: ShaderInstance? = null
    var PARTICLE: ShaderInstance? = null
    var UI_IMAGE_SHADOW: ShaderInstance? = null
    var MSDF_TEXT: ShaderInstance? = null

    @SubscribeEvent
    fun onShaderRegistry(event: RegisterShadersEvent) {
        event.register("$MODID:legacy/position_color".rl, DefaultVertexFormat.POSITION_COLOR) { POSITION_COLOR = it }
        event.register("$MODID:legacy/position_tex".rl, DefaultVertexFormat.POSITION_TEX) { POSITION_TEX = it }
        event.register("$MODID:legacy/position_tex_color".rl, DefaultVertexFormat.POSITION_TEX_COLOR) {
            POSITION_TEX_COLOR = it
        }
        event.register("$MODID:legacy/particle".rl, DefaultVertexFormat.PARTICLE) { PARTICLE = it }
        event.register(
            "$MODID:gltf_entity-1.21.1".rl,
            DefaultVertexFormat.NEW_ENTITY
        ) {
            GLTF_ENTITY = it
        }
        event.register(
            "$MODID:gltf_entity_instanced-1.21.1".rl,
            DefaultVertexFormat.NEW_ENTITY
        ) {
            GLTF_ENTITY_INSTANCED = it
        }
        event.register(
            "$MODID:vfx_particle".rl,
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) {
            VFX_PARTICLE = it
            VfxQuadRenderer.invalidate()
        }
        event.register(
            "$MODID:vfx_mesh".rl,
            DefaultVertexFormat.POSITION_TEX
        ) {
            VFX_MESH = it
            VfxMeshRenderer.invalidate()
        }
        event.register("$MODID:vfx_ribbon".rl, DefaultVertexFormat.PARTICLE) {
            VFX_RIBBON = it
        }
        event.register(
            "$MODID:ui_effect".rl,
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) {
            UI_EFFECT = it
        }
        event.register(
            "$MODID:msdf_text".rl,
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) {
            MSDF_TEXT = it
        }
        event.register("$MODID:ui_image_shadow".rl, DefaultVertexFormat.POSITION_TEX_COLOR) {
            UI_IMAGE_SHADOW = it
        }
    }
}
