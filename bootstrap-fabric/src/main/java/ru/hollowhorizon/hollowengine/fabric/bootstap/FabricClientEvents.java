package ru.hollowhorizon.hollowengine.fabric.bootstap;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.packs.PackType;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;
import ru.hollowhorizon.hollowengine.bootstrap.impl.LevelStageDispatcher;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.RuntimeBridge.RenderLevelStage;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.EventBridge;
import ru.hollowhorizon.hollowengine.fabric.internal.DelegatedReloadListener;

public class FabricClientEvents {
    private static final EventBridge events = BootstrapRuntimeManager.bridge().events();

    public static void init() {
        registerReloadListeners();
        registerLevelStages();
        FabricHudLayers.init();
        events.onRegisterEntityRenderers(EntityRenderers::register);
        events.onRegisterBlockEntityRenderers(BlockEntityRenderers::register);
        ItemTooltipCallback.EVENT.register(events::onGetTooltip);
        events.onRegisterKeybindings(KeyMappingHelper::registerKeyMapping);
        ClientTickEvents.END_CLIENT_TICK.register(events::onClientTick);
        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext) ->
                events.onClientCommandRegistration((CommandDispatcher<SharedSuggestionProvider>) (Object) commandDispatcher, commandBuildContext));
    }


    private static void registerReloadListeners() {
        events.onRegisterClientReloadListeners(listener -> DelegatedReloadListener.register(PackType.CLIENT_RESOURCES, listener));
    }

    private static void registerLevelStages() {
        LevelRenderEvents.START_MAIN.register(ctx -> LevelStageDispatcher.fire(ctx.levelRenderer(), ctx.levelState(), null, RenderLevelStage.AFTER_SKY));
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(ctx -> LevelStageDispatcher.fire(ctx.levelRenderer(), ctx.levelState(), null,
            RenderLevelStage.AFTER_SOLID_BLOCKS, RenderLevelStage.AFTER_CUTOUT_MIPPED_BLOCKS, RenderLevelStage.AFTER_CUTOUT_BLOCKS));
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(ctx -> LevelStageDispatcher.fire(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(),
            RenderLevelStage.AFTER_ENTITIES, RenderLevelStage.AFTER_BLOCK_ENTITIES));
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx -> LevelStageDispatcher.fire(ctx.levelRenderer(), ctx.levelState(), null,
            RenderLevelStage.AFTER_TRANSLUCENT_BLOCKS, RenderLevelStage.AFTER_TRIPWIRE_BLOCKS));
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> LevelStageDispatcher.fire(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(),
            RenderLevelStage.AFTER_PARTICLES, RenderLevelStage.AFTER_WEATHER));
        LevelRenderEvents.END_MAIN.register(ctx -> LevelStageDispatcher.fire(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), RenderLevelStage.AFTER_LEVEL));
    }
}
