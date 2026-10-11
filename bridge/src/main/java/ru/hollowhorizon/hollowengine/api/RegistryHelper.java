package ru.hollowhorizon.hollowengine.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public interface RegistryHelper {
    <T> ResourceKey<? extends Registry<T>> registry(Class<T> type);

    void addBlockModel(Identifier location, AutoModelType model);
    void addItemModel(Identifier location, AutoModelType model);
}
