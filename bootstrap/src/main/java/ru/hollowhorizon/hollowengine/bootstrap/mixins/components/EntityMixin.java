package ru.hollowhorizon.hollowengine.bootstrap.mixins.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import ru.hollowhorizon.hollowengine.api.extensions.EntityExtension;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityExtension {
    @Shadow private Level level;
    @Shadow public abstract Level level();

    @Unique
    @Nullable
    private Object hollowengine$detachedAttachments;

    @Override
    public @Nullable Object hollowengine$detachedAttachments() {
        return hollowengine$detachedAttachments;
    }

    @Override
    public void hollowengine$setDetachedAttachments(@Nullable Object attachments) {
        hollowengine$detachedAttachments = attachments;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void onSave(ValueOutput output, CallbackInfo ci) {
        if (output instanceof TagValueOutputAccessor accessor) {
            BootstrapRuntimeManager.bridge().onEntitySaved((Entity) (Object) this, accessor.hollowengine$getOutput());
            return;
        }
        CompoundTag tag = new CompoundTag();
        BootstrapRuntimeManager.bridge().onEntitySaved((Entity) (Object) this, tag);
        for (String key : tag.keySet()) {
            output.store(key, ExtraCodecs.NBT, tag.get(key));
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(ValueInput input, CallbackInfo ci) {
        CompoundTag tag = input instanceof TagValueInputAccessor accessor ? accessor.hollowengine$getInput() : new CompoundTag();
        BootstrapRuntimeManager.bridge().onEntityLoaded((Entity) (Object) this, tag);
    }

    // LivingEntity damage is hooked in LivingEntityMixin#hurtServer, so only plain entities are handled here.
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource damageSource, float amount, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity) return;
        if (BootstrapRuntimeManager.bridge().onEntityHurt((Entity) (Object) this, damageSource, amount)) {
            ci.cancel();
        }
    }

    @Inject(method = "hurtOrSimulate", at = @At("HEAD"), cancellable = true)
    private void onHurtOrSimulate(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity) return;
        if (BootstrapRuntimeManager.bridge().onEntityHurt((Entity) (Object) this, damageSource, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "teleportCrossDimension", at = @At("RETURN"))
    private void onChangeDimension(ServerLevel oldLevel, ServerLevel newLevel, TeleportTransition transition, CallbackInfoReturnable<Entity> cir) {
        BootstrapRuntimeManager.bridge().onEntityChangedDimension((Entity) (Object) this, cir.getReturnValue(), oldLevel, newLevel);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onSetLevel(Level level, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onEntitySetLevel((Entity) (Object) this, level);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason removalReason, CallbackInfo ci) {
        BootstrapRuntimeManager.bridge().onEntityRemoved((Entity) (Object) this);
    }
}
