package ru.hollowhorizon.hollowengine.testing

import net.minecraft.SharedConstants
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.server.Bootstrap

/**
 * What a test needs before it can make an item stack. Since 26.1 an item's components are bound while a
 * world's data is loaded, not when the registry freezes, so they are bound here the way a world would.
 */
object MinecraftTestBootstrap {
    private var done = false

    @Synchronized
    fun init() {
        if (done) return
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
        done = true
    }
}
