package ru.hollowhorizon.hollowengine.bootstrap.mixins.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.hollowhorizon.hollowengine.api.extensions.EntityRenderStateExtension;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateExtension {
    @Unique
    private @Nullable Entity hollowengine$entity;
    @Unique
    private float hollowengine$partialTick;

    @Override
    public @Nullable Entity hollowengine$entity() {
        return hollowengine$entity;
    }

    @Override
    public float hollowengine$partialTick() {
        return hollowengine$partialTick;
    }

    @Override
    public void hollowengine$setEntity(@Nullable Entity entity, float partialTick) {
        hollowengine$entity = entity;
        hollowengine$partialTick = partialTick;
    }
}
