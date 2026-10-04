package ru.hollowhorizon.hollowengine.common.events.registry

import ru.hollowhorizon.hollowengine.client.render.legacy.VertexFormat
import ru.hollowhorizon.hollowengine.client.render.legacy.ShaderInstance
import net.minecraft.resources.Identifier
import ru.hollowhorizon.hollowengine.common.events.ClientEvent
import ru.hollowhorizon.hollowengine.common.events.factory.EventHandler

class RegisterShadersEvent : ClientEvent {
    companion object : EventHandler<RegisterShadersEvent>()

    val shaders = hashMapOf<Identifier, Pair<VertexFormat, (ShaderInstance) -> Unit>>()

    fun register(location: Identifier, format: VertexFormat, consumer: (ShaderInstance) -> Unit) {
        shaders += location to Pair(format, consumer)
    }
}