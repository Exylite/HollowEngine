package ru.hollowhorizon.hollowengine.fabric.bootstap;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.HudLayerIds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wraps every vanilla HUD element so the runtime gets the same per-layer pre/post hooks NeoForge exposes
 * through its layer events.
 */
final class FabricHudLayers {
    private FabricHudLayers() {
    }

    static void init() {
        Map<Identifier, String> layers = new LinkedHashMap<>();
        layers.put(VanillaHudElements.MISC_OVERLAYS, HudLayerIds.CAMERA_OVERLAYS);
        layers.put(VanillaHudElements.CROSSHAIR, HudLayerIds.CROSSHAIR);
        layers.put(VanillaHudElements.HOTBAR, HudLayerIds.HOTBAR);
        layers.put(VanillaHudElements.ARMOR_BAR, HudLayerIds.ARMOR_LEVEL);
        layers.put(VanillaHudElements.HEALTH_BAR, HudLayerIds.PLAYER_HEALTH);
        layers.put(VanillaHudElements.FOOD_BAR, HudLayerIds.FOOD_LEVEL);
        layers.put(VanillaHudElements.AIR_BAR, HudLayerIds.AIR_LEVEL);
        layers.put(VanillaHudElements.MOUNT_HEALTH, HudLayerIds.VEHICLE_HEALTH);
        layers.put(VanillaHudElements.INFO_BAR, HudLayerIds.EXPERIENCE_BAR);
        layers.put(VanillaHudElements.EXPERIENCE_LEVEL, HudLayerIds.EXPERIENCE_LEVEL);
        layers.put(VanillaHudElements.HELD_ITEM_TOOLTIP, HudLayerIds.SELECTED_ITEM_NAME);
        layers.put(VanillaHudElements.SPECTATOR_TOOLTIP, HudLayerIds.SPECTATOR_TOOLTIP);
        layers.put(VanillaHudElements.MOB_EFFECTS, HudLayerIds.EFFECTS);
        layers.put(VanillaHudElements.BOSS_BAR, HudLayerIds.BOSS_OVERLAY);
        layers.put(VanillaHudElements.SLEEP, HudLayerIds.SLEEP_OVERLAY);
        layers.put(VanillaHudElements.DEMO_TIMER, HudLayerIds.DEMO_OVERLAY);
        layers.put(VanillaHudElements.SCOREBOARD, HudLayerIds.SCOREBOARD_SIDEBAR);
        layers.put(VanillaHudElements.OVERLAY_MESSAGE, HudLayerIds.OVERLAY_MESSAGE);
        layers.put(VanillaHudElements.TITLE_AND_SUBTITLE, HudLayerIds.TITLE);
        layers.put(VanillaHudElements.CHAT, HudLayerIds.CHAT);
        layers.put(VanillaHudElements.PLAYER_LIST, HudLayerIds.TAB_LIST);
        layers.put(VanillaHudElements.SUBTITLES, HudLayerIds.SUBTITLE_OVERLAY);
        layers.forEach((id, layer) -> HudElementRegistry.replaceElement(id, original -> wrap(original, layer)));
    }

    private static HudElement wrap(HudElement original, String layer) {
        return (graphics, deltaTracker) -> {
            var window = Minecraft.getInstance().getWindow();
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            var bridge = BootstrapRuntimeManager.bridge();
            if (bridge.onRenderOverlayPre(window, graphics, partialTick, layer)) return;
            original.extractRenderState(graphics, deltaTracker);
            bridge.onRenderOverlayPost(window, graphics, partialTick, layer);
        };
    }
}
