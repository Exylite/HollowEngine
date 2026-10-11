package ru.hollowhorizon.hollowengine.client.render.legacy

import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import ru.hollowhorizon.hollowengine.HollowEngine
import ru.hollowhorizon.hollowengine.common.events.registry.RegisterShadersEvent

/**
 * Builds the programs that [RegisterShadersEvent] collects whenever the resources are loaded:
 * vanilla's own shader loading knows nothing about them any more.
 */
object LegacyShaderLoader : ResourceManagerReloadListener {
    private val loaded = ArrayList<ShaderInstance>()

    override fun onResourceManagerReload(manager: ResourceManager) {
        loaded.forEach(ShaderInstance::close)
        loaded.clear()

        val event = RegisterShadersEvent.post(RegisterShadersEvent())
        for ((id, entry) in event.shaders) {
            val (format, consumer) = entry
            try {
                val shader = ShaderInstance(manager, id.toString(), format)
                loaded += shader
                consumer(shader)
            } catch (e: Exception) {
                HollowEngine.LOGGER.error("Could not load the shader {}", id, e)
            }
        }
    }
}
