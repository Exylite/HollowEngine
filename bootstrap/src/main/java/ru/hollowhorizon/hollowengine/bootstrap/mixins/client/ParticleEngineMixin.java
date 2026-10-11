package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.particle.ParticleResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

// 26.x moved particle providers from ParticleEngine to ParticleResources.
@Mixin(ParticleResources.class)
public class ParticleEngineMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    public void hollowengine$init(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onRegisterParticles((ParticleResources) (Object) this);
    }
}
