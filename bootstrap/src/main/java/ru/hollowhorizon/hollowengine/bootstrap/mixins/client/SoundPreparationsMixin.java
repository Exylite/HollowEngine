package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

import java.util.HashMap;
import java.util.Map;

/** Adds the sound files the game would not list itself to what it has found, once it has listed its own. */
@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class SoundPreparationsMixin {
    @Shadow
    private Map<Identifier, Resource> soundCache;

    @Inject(method = "listResources", at = @At("TAIL"))
    private void hollowengine$listExtraSounds(ResourceManager resourceManager, CallbackInfo ci) {
        Map<Identifier, Resource> merged = new HashMap<>(soundCache);
        merged.putAll(BootstrapRuntimeManager.bridge().listExtraSounds(resourceManager));
        soundCache = merged;
    }
}
