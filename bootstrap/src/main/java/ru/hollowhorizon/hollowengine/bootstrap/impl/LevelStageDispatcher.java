package ru.hollowhorizon.hollowengine.bootstrap.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jetbrains.annotations.Nullable;
import ru.hollowhorizon.hollowengine.bootstrap.runtime.RuntimeBridge;

/**
 * Translates the loader's level render phases (26.x renders the level through a frame graph, so stages are
 * exposed by Fabric API / NeoForge events instead of fixed call sites) into the runtime's legacy stage hooks.
 */
public final class LevelStageDispatcher {
    private LevelStageDispatcher() {
    }

    public static void fire(LevelRenderer renderer, LevelRenderState state, @Nullable PoseStack poseStack, RuntimeBridge.RenderLevelStage... stages) {
        Minecraft minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.mainCamera();
        var cameraState = state.cameraRenderState;
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack stack = poseStack != null ? poseStack : new PoseStack();
        for (RuntimeBridge.RenderLevelStage stage : stages) {
            BootstrapRuntimeManager.bridge().onRenderLevelStage(renderer, stack, cameraState.projectionMatrix, (int) state.gameTime, partialTick, camera, cameraState.cullFrustum, stage);
        }
    }
}
