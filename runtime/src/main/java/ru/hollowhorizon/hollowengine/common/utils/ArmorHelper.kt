package ru.hollowhorizon.hollowengine.common.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

/** Where the worn texture of an armor piece is: equipment assets name it, and legs have a layer of their own. */
fun ItemStack.getArmorTexture(entity: Entity, slot: EquipmentSlot): Identifier {
    val asset = get(DataComponents.EQUIPPABLE)?.assetId()?.getOrNull()?.identifier()
        ?: return Identifier.withDefaultNamespace("missing")
    val layer = if (slot == EquipmentSlot.LEGS) "humanoid_leggings" else "humanoid"
    return Identifier.fromNamespaceAndPath(asset.namespace, "textures/entity/equipment/$layer/${asset.path}.png")
}
