package ru.hollowhorizon.hollowengine.bootstrap.mixins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(value = MinecraftServer.class, priority = 993)
public abstract class MinecraftServerMixin {
    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    @Final
    private Thread serverThread;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onServerCreated((MinecraftServer) (Object) this, serverThread, storageSource.getLevelPath(LevelResource.ROOT));
    }

    @Inject(method = "runServer", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onServerStarting((MinecraftServer) (Object) this);
    }

    @Inject(method = "createLevels", at = @At("TAIL"))
    private void onCreateLevels(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onServerLevelsCreated((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTickServer(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onServerTick((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void beforeStopServer(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onServerStopping((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("RETURN"))
    private void afterStopServer(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onServerStopped((MinecraftServer) (Object) this);
    }
}
