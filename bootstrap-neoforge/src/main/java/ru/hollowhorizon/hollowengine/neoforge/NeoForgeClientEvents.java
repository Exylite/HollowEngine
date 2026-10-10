package ru.hollowhorizon.hollowengine.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;
import ru.hollowhorizon.hollowengine.bootstrap.impl.LevelStageDispatcher;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.RuntimeBridge.RenderLevelStage;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.EventBridge;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.RuntimeBridge;


public class NeoForgeClientEvents {
    private static final RuntimeBridge bridge = BootstrapRuntimeManager.bridge();
    private static final EventBridge events = bridge.events();

    public static void init(IEventBus modBus) {
        var forgeBus = NeoForge.EVENT_BUS;
        forgeBus.addListener(NeoForgeClientEvents::onRenderTooltips);
        forgeBus.addListener(NeoForgeClientEvents::onClientTick);
        forgeBus.addListener(NeoForgeClientEvents::registerClientCommands);
        forgeBus.addListener(NeoForgeClientEvents::onRenderOverlayPre);
        forgeBus.addListener(NeoForgeClientEvents::onRenderOverlayPost);
        forgeBus.addListener(NeoForgeClientEvents::onRenderHudPost);
        forgeBus.addListener(NeoForgeClientEvents::onCameraSetup);
        forgeBus.addListener(NeoForgeClientEvents::onRenderArm);
        forgeBus.addListener((RenderLevelStageEvent.AfterSky event) -> stage(event, RenderLevelStage.AFTER_SKY));
        forgeBus.addListener((RenderLevelStageEvent.AfterOpaqueBlocks event) -> stage(event,
            RenderLevelStage.AFTER_SOLID_BLOCKS, RenderLevelStage.AFTER_CUTOUT_MIPPED_BLOCKS, RenderLevelStage.AFTER_CUTOUT_BLOCKS));
        forgeBus.addListener((RenderLevelStageEvent.AfterOpaqueFeatures event) -> stage(event,
            RenderLevelStage.AFTER_ENTITIES, RenderLevelStage.AFTER_BLOCK_ENTITIES));
        forgeBus.addListener((RenderLevelStageEvent.AfterTranslucentBlocks event) -> stage(event,
            RenderLevelStage.AFTER_TRANSLUCENT_BLOCKS, RenderLevelStage.AFTER_TRIPWIRE_BLOCKS));
        forgeBus.addListener((RenderLevelStageEvent.AfterTranslucentParticles event) -> stage(event, RenderLevelStage.AFTER_PARTICLES));
        forgeBus.addListener((RenderLevelStageEvent.AfterWeather event) -> stage(event, RenderLevelStage.AFTER_WEATHER));
        forgeBus.addListener((RenderLevelStageEvent.AfterLevel event) -> stage(event, RenderLevelStage.AFTER_LEVEL));

        modBus.addListener(NeoForgeClientEvents::registerRenderers);
        modBus.addListener(NeoForgeClientEvents::registerKeyMappings);
        modBus.addListener(NeoForgeClientEvents::registerReloadListeners);

    }

    private static void registerReloadListeners(AddClientReloadListenersEvent event) {
        events.onRegisterClientReloadListeners(listener -> event.addListener(ReloadListenerIds.idFor(listener), listener));
    }

    @SuppressWarnings("unchecked")
    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        events.onClientCommandRegistration((CommandDispatcher<SharedSuggestionProvider>) (Object) event.getDispatcher(), event.getBuildContext());
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        events.onRegisterEntityRenderers(event::registerEntityRenderer);
        events.onRegisterBlockEntityRenderers(event::registerBlockEntityRenderer);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        events.onRegisterKeybindings(event::register);
    }

    private static void onRenderTooltips(ItemTooltipEvent event) {
        events.onGetTooltip(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        events.onClientTick(Minecraft.getInstance());
    }

    private static void onRenderOverlayPre(RenderGuiLayerEvent.Pre event) {
        // Forward every named layer by its id; the runtime decides which ones it cares about. This
        // covers all vanilla layers and any modded layer without a per-layer mapping here.
        if (bridge.onRenderOverlayPre(Minecraft.getInstance().getWindow(), event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getName().toString())) {
            event.setCanceled(true);
        }
    }

    private static void onRenderOverlayPost(RenderGuiLayerEvent.Post event) {
        bridge.onRenderOverlayPost(Minecraft.getInstance().getWindow(), event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getName().toString());
    }

    private static void onRenderHudPost(RenderGuiEvent.Post event) {
        bridge.onRenderHudPost(Minecraft.getInstance().getWindow(), event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    private static void onRenderArm(RenderArmEvent<?> event) {
        if (!(event.getAvatar() instanceof AbstractClientPlayer player)) return;
        if (bridge.onRenderArm(event.getPoseStack(), event.getSubmitNodeCollector(), event.getLightCoords(), player, event.getArm())) {
            event.setCanceled(true);
        }
    }

    private static void stage(RenderLevelStageEvent event, RenderLevelStage... stages) {
        LevelStageDispatcher.fire(event.getLevelRenderer(), event.getLevelRenderState(), event.getPoseStack(), stages);
    }

    private static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        var setup = bridge.onCameraSetup(event.getRenderer(), event.getCamera(), event.getYaw(), event.getPitch(), event.getRoll(), (float) event.getPartialTick());
        event.setYaw(setup.yaw());
        event.setPitch(setup.pitch());
        event.setRoll(setup.roll());
    }
}
