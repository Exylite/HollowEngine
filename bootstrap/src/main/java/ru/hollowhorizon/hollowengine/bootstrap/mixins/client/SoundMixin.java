package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Sound.class)
public class SoundMixin {
    @Shadow
    @Final
    private Identifier location;

    @Inject(method = "getPath", at = @At("HEAD"), cancellable = true)
    private void hollowengine$extendedPath(CallbackInfoReturnable<Identifier> cir) {
        Identifier file = BootstrapRuntimeManager.bridge().extendedSoundFile(location);
        if (file != null) {
            cir.setReturnValue(file);
        }
    }
}
