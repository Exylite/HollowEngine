package ru.hollowhorizon.hollowengine.common.utils

import net.minecraft.world.item.CreativeModeTab

object HollowCreativeTab {
    @JvmStatic
    fun builder(): CreativeModeTab.Builder = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
}
