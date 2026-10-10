package ru.hollowhorizon.hollowengine.common.events.registry

import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleResources
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import ru.hollowhorizon.hollowengine.common.events.ClientEvent
import ru.hollowhorizon.hollowengine.common.events.StartupEvent
import ru.hollowhorizon.hollowengine.common.events.factory.EventHandler

class RegisterParticlesEvent(val particleResources: ParticleResources) : ClientEvent, StartupEvent {
    companion object : EventHandler<RegisterParticlesEvent>()

    fun <T : ParticleOptions> registerSpecial(type: ParticleType<T>, provider: ParticleProvider<T>) {
        particleResources.register(type, provider)
    }

    /** A provider that builds a single-quad particle: vanilla only takes the general one now, which it extends. */
    fun <T : ParticleOptions> registerSprite(type: ParticleType<T>, provider: ParticleProvider.Sprite<T>) {
        particleResources.register(type, ParticleProvider<T> { options, level, x, y, z, xAux, yAux, zAux, random ->
            provider.createParticle(options, level, x, y, z, xAux, yAux, zAux, random)
        })
    }

    fun <T : ParticleOptions> registerSpriteSet(
        type: ParticleType<T>,
        provider: ParticleResources.SpriteParticleRegistration<T>,
    ) {
        particleResources.register(type, provider)
    }
}
