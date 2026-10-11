package ru.hollowhorizon.hollowengine.bootstrap.mixins.components;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onLevelCreated((Level) (Object) this);
    }

    @Inject(method = "updateNeighborsAt", at = @At("HEAD"))
    private void onUpdateNeighbors(BlockPos pos, Block block, @Nullable Orientation orientation, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onLevelUpdateNeighbors((Level) (Object) this, pos);
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onLevelTickBlockEntities((Level) (Object) this);
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void onClose(CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onLevelClosed((Level) (Object) this);
    }
}
