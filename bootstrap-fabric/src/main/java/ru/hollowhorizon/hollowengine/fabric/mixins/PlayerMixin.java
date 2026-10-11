package ru.hollowhorizon.hollowengine.fabric.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.hollowhorizon.hollowengine.bootstrap.impl.BootstrapRuntimeManager;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onDrop(ItemStack itemStack, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity dropped = ((LivingEntity) (Object) this).drop(itemStack, false, includeThrowerName);
        cir.setReturnValue(BootstrapRuntimeManager.bridge().onPlayerDrop(itemStack, includeThrowerName, dropped, (Player) (Object) this));
    }
}
