@file:Suppress("UNCHECKED_CAST")
package ru.hollowhorizon.hollowengine.common.registry

import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.resources.Identifier
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Items
import ru.hollowhorizon.hollowengine.HollowEngine.MODID
import ru.hollowhorizon.hollowengine.api.AutoModelType
import ru.hollowhorizon.hollowengine.api.RegistryHolder
import ru.hollowhorizon.hollowengine.common.utils.HollowCreativeTab
import ru.hollowhorizon.hollowengine.common.utils.mcTranslate
import ru.hollowhorizon.hollowengine.common.utils.rl
import kotlin.reflect.KProperty

open class HollowRegistry(val modId: String = MODID) {
    /**
     * Avoid fake NotNulls parameters like BlockEntityType.Builder::build
     */
    fun <T> promise(): T = null as T

    inline fun <reified T : Any> register(
        location: Identifier,
        autoModel: AutoModelType? = AutoModelType.DEFAULT,
        registry: Registry<in T>? = null,
        noinline registryEntry: (Identifier) -> T,
    ): RegistryHolder<T> {
        return CommonRegistryProvider.register(
            location,
            registry as Registry<Any>?,
            autoModel,
            { registryEntry(location) },
            T::class.java as Class<Any>
        ) as RegistryHolder<T>
    }

    inline fun <reified T : Any> register(
        id: String,
        autoModel: AutoModelType? = AutoModelType.DEFAULT,
        registry: Registry<in T>? = null,
        noinline registryEntry: (Identifier) -> T,
    ): RegistryHolder<T> = register(location(id), autoModel, registry, registryEntry)

    /** A plain [id] lands in [modId]; a full `namespace:path` is used as written. */
    fun location(id: String): Identifier = if (':' in id) id.rl else "$modId:$id".rl

    /** Item properties already carrying the id the item is registered under: the game refuses an item without one. */
    fun itemProperties(id: Identifier): Item.Properties =
        Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))

    /** The same for blocks. */
    fun blockProperties(id: Identifier): BlockBehaviour.Properties =
        BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))

    fun creativeTab(name: String, block: CreativeModeTab.Builder.() -> Unit = {}) = register(name) {
        HollowCreativeTab.builder()
            .icon { Items.DIRT.defaultInstance }
            .title("itemGroup.$name".mcTranslate)
            .apply { block() }
            .build()
    }
}

operator fun <T> RegistryHolder<T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
