package ru.hollowhorizon.hollowengine.bootstrap.mixins.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueOutput.class)
public interface TagValueOutputAccessor {
    @Accessor("output")
    CompoundTag hollowengine$getOutput();
}
