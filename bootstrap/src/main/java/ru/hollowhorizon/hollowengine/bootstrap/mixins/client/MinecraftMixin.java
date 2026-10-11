package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.systems.GpuBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PreferredGraphicsApi;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Nullable public ClientLevel level;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(GameConfig gameConfig, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onClientCreated((Minecraft) (Object) this);
    }

    // The engine renders through raw OpenGL, so the Vulkan backend is never offered.
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/PreferredGraphicsApi;getBackendsToTry()[Lcom/mojang/blaze3d/systems/GpuBackend;"))
    private GpuBackend[] hollowengine$forceOpenGl(PreferredGraphicsApi api) {
        return new GpuBackend[]{new GlBackend()};
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRunTickHead(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onClientTick((Minecraft) (Object) this);
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void onRenderPre(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onClientRenderTickPre((Minecraft) (Object) this);
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", shift = At.Shift.AFTER))
    private void onRenderPost(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onClientRenderTickPost((Minecraft) (Object) this);
    }

    @Inject(method = "renderFrame", at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"
    ))
    private void beforeBlit(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onBeforeBlitScreen((Minecraft) (Object) this);
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V", shift = At.Shift.AFTER))
    private void afterBlit(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onBlitScreen((Minecraft) (Object) this);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStopHead(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onClientStopping((Minecraft) (Object) this);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onSetClientLevel(ClientLevel newLevel, CallbackInfo ci) {
        if (level != newLevel) hollowengine$releaseLevel();
    }

    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void onClearClientLevel(Screen screen, CallbackInfo ci) {
        hollowengine$releaseLevel();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean keepResourcePacks, boolean stopSound, CallbackInfo ci) {
        hollowengine$releaseLevel();
    }

    private void hollowengine$releaseLevel() {
        if (level != null) BootstrapRuntimeManager.bridge().onLevelClosed(level);
    }
}
