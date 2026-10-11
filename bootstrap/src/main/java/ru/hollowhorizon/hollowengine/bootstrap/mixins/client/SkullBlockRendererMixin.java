package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(SkullBlockRenderer.class)
public class SkullBlockRendererMixin {
    // Vanilla resolves skull models lazily per type; custom types fall through to null, so ask the runtime.
    @Inject(method = "createModel", at = @At("RETURN"), cancellable = true)
    private static void onCreateModel(EntityModelSet modelSet, SkullBlock.Type type, CallbackInfoReturnable<SkullModelBase> cir) {
        if (cir.getReturnValue() != null) return;
        ImmutableMap.Builder<SkullBlock.Type, SkullModelBase> builder = ImmutableMap.builder();
        BootstrapRuntimeManager.bridge().onCreateSkullModels(builder, modelSet);
        SkullModelBase model = builder.buildKeepingLast().get(type);
        if (model != null) cir.setReturnValue(model);
    }
}
