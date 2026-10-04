package ru.hollowhorizon.hollowengine.common.ui.hud

import net.minecraft.resources.Identifier
import ru.hollowhorizon.hollowengine.bootstrap.runtime.HudLayerIds
import ru.hollowhorizon.hollowengine.common.ui.hud.VanillaHudLayers.AIR_LEVEL
import ru.hollowhorizon.hollowengine.common.ui.hud.VanillaHudLayers.ARMOR_LEVEL
import ru.hollowhorizon.hollowengine.common.ui.hud.VanillaHudLayers.FOOD_LEVEL

/**
 * Identifiers for the vanilla HUD layers the engine can anchor to or hide. These are the real
 * vanilla layer resource locations (see [HudLayerIds]) rather than an enum, so on NeoForge a layer's
 * id passes straight through `RenderGuiLayerEvent.getName()`.
 *
 * Coverage differs by platform: NeoForge forwards every named layer, so all of these fire there.
 * Fabric fires the subset its mixins hook (the camera overlays plus the common status bars);
 * TODO: a few * NeoForge-only layers (e.g. [FOOD_LEVEL], [AIR_LEVEL], [ARMOR_LEVEL]) are drawn together inside
 *  `renderPlayerHealth` on Fabric and so are not separable there.
 */
object VanillaHudLayers {
    val CAMERA_OVERLAYS = Identifier.parse(HudLayerIds.CAMERA_OVERLAYS)
    val CROSSHAIR = Identifier.parse(HudLayerIds.CROSSHAIR)
    val HOTBAR = Identifier.parse(HudLayerIds.HOTBAR)
    val JUMP_METER = Identifier.parse(HudLayerIds.JUMP_METER)
    val EXPERIENCE_BAR = Identifier.parse(HudLayerIds.EXPERIENCE_BAR)
    val EXPERIENCE_LEVEL = Identifier.parse(HudLayerIds.EXPERIENCE_LEVEL)
    val PLAYER_HEALTH = Identifier.parse(HudLayerIds.PLAYER_HEALTH)
    val ARMOR_LEVEL = Identifier.parse(HudLayerIds.ARMOR_LEVEL)
    val FOOD_LEVEL = Identifier.parse(HudLayerIds.FOOD_LEVEL)
    val AIR_LEVEL = Identifier.parse(HudLayerIds.AIR_LEVEL)
    val VEHICLE_HEALTH = Identifier.parse(HudLayerIds.VEHICLE_HEALTH)
    val SELECTED_ITEM_NAME = Identifier.parse(HudLayerIds.SELECTED_ITEM_NAME)
    val SPECTATOR_TOOLTIP = Identifier.parse(HudLayerIds.SPECTATOR_TOOLTIP)
    val EFFECTS = Identifier.parse(HudLayerIds.EFFECTS)
    val BOSS_OVERLAY = Identifier.parse(HudLayerIds.BOSS_OVERLAY)
    val SLEEP_OVERLAY = Identifier.parse(HudLayerIds.SLEEP_OVERLAY)
    val DEMO_OVERLAY = Identifier.parse(HudLayerIds.DEMO_OVERLAY)
    val DEBUG_OVERLAY = Identifier.parse(HudLayerIds.DEBUG_OVERLAY)
    val SCOREBOARD_SIDEBAR = Identifier.parse(HudLayerIds.SCOREBOARD_SIDEBAR)
    val OVERLAY_MESSAGE = Identifier.parse(HudLayerIds.OVERLAY_MESSAGE)
    val TITLE = Identifier.parse(HudLayerIds.TITLE)
    val SUBTITLE_OVERLAY = Identifier.parse(HudLayerIds.SUBTITLE_OVERLAY)
    val CHAT = Identifier.parse(HudLayerIds.CHAT)
    val TAB_LIST = Identifier.parse(HudLayerIds.TAB_LIST)
    val SAVING_INDICATOR = Identifier.parse(HudLayerIds.SAVING_INDICATOR)

    val VIGNETTE = Identifier.parse(HudLayerIds.VIGNETTE)
    val SPYGLASS = Identifier.parse(HudLayerIds.SPYGLASS)
    val HELMET = Identifier.parse(HudLayerIds.HELMET)
    val FROSTBITE = Identifier.parse(HudLayerIds.FROSTBITE)
    val PORTAL = Identifier.parse(HudLayerIds.PORTAL)

    val all: Set<Identifier> = setOf(
        CAMERA_OVERLAYS, CROSSHAIR, HOTBAR, JUMP_METER, EXPERIENCE_BAR, EXPERIENCE_LEVEL, PLAYER_HEALTH,
        ARMOR_LEVEL, FOOD_LEVEL, AIR_LEVEL, VEHICLE_HEALTH, SELECTED_ITEM_NAME, SPECTATOR_TOOLTIP, EFFECTS,
        BOSS_OVERLAY, SLEEP_OVERLAY, DEMO_OVERLAY, DEBUG_OVERLAY, SCOREBOARD_SIDEBAR, OVERLAY_MESSAGE, TITLE,
        SUBTITLE_OVERLAY, CHAT, TAB_LIST, SAVING_INDICATOR, VIGNETTE, SPYGLASS, HELMET, FROSTBITE, PORTAL,
    )

    /** Parses a layer id, accepting bare vanilla names (`crosshair`) as well as full ids. */
    fun parse(name: String): Identifier =
        if (':' in name) Identifier.parse(name) else Identifier.withDefaultNamespace(name)
}
