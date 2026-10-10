package ru.hollowhorizon.hollowengine.api.extensions;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Links an {@link EntityRenderState} back to the entity it was extracted from, so render hooks that
 * run during submission (after extraction) can still reach the entity.
 */
public interface EntityRenderStateExtension {
    @Nullable
    Entity hollowengine$entity();

    float hollowengine$partialTick();

    void hollowengine$setEntity(@Nullable Entity entity, float partialTick);

    @Nullable
    static Entity entityOf(EntityRenderState state) {
        return ((EntityRenderStateExtension) state).hollowengine$entity();
    }
}
