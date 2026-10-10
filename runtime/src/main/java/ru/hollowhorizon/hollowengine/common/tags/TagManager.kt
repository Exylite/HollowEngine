package ru.hollowhorizon.hollowengine.common.tags

import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.level.block.Block
import ru.hollowhorizon.hollowengine.common.events.SubscribeEvent
import ru.hollowhorizon.hollowengine.common.events.registry.RegisterTagsEvent
import ru.hollowhorizon.hollowengine.common.utils.rl

object TagManager {
    val BLOCK_TAGS = HashMap<TagKey<Block>, MutableSet<Block>>()
    val ITEM_TAGS = HashMap<TagKey<Item>, MutableSet<Item>>()

    @SubscribeEvent
    fun registerTags(e: RegisterTagsEvent) {
        if(e.registry.key() == Registries.BLOCK) {
            BLOCK_TAGS.forEach { (tags, blocks) ->
                e.addToTag(tags, *blocks.toTypedArray())
            }
        }
        if(e.registry.key() == Registries.ITEM) {
            ITEM_TAGS.forEach { (tags, blocks) ->
                e.addToTag(tags, *blocks.toTypedArray())
            }
        }
    }
}

fun Block.addTag(tag: Identifier) = addTag(TagKey.create(Registries.BLOCK, tag))
fun Block.addTag(tag: TagKey<Block>) = TagManager.BLOCK_TAGS.getOrPut(tag, ::HashSet).add(this)

fun Item.addTag(tag: Identifier) {
    val tagKey = TagKey.create(Registries.ITEM, tag)
    TagManager.ITEM_TAGS.getOrPut(tagKey, ::HashSet).add(this)
}

fun Block.addTool(type: ToolType, tier: ToolMaterial) {
    val tag = when (tier) {
        ToolMaterial.WOOD -> TagKey.create(Registries.BLOCK, "needs_wood_tool".rl) // Only forge?
        ToolMaterial.GOLD -> TagKey.create(Registries.BLOCK, "needs_gold_tool".rl) // Only forge?
        ToolMaterial.STONE -> BlockTags.NEEDS_STONE_TOOL
        ToolMaterial.IRON -> BlockTags.NEEDS_IRON_TOOL
        ToolMaterial.DIAMOND -> BlockTags.NEEDS_DIAMOND_TOOL
        ToolMaterial.NETHERITE -> TagKey.create(Registries.BLOCK, "needs_netherite_tool".rl) // Only forge?
        ToolMaterial.COPPER -> TagKey.create(Registries.BLOCK, "needs_copper_tool".rl) // Only forge?
        else -> error("Unknown tier: $tier")
    }
    addTag(tag)
    addTag(type.tag)
}

interface ToolType {
    val tag: TagKey<Block>
}

enum class ToolTypes(override val tag: TagKey<Block>) : ToolType {
    AXE(BlockTags.MINEABLE_WITH_AXE),
    HOE(BlockTags.MINEABLE_WITH_HOE),
    PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
    SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL)
}
