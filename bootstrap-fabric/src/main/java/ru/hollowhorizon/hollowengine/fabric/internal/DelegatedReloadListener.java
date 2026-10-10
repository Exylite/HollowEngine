package ru.hollowhorizon.hollowengine.fabric.internal;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Locale;

public final class DelegatedReloadListener {
    private DelegatedReloadListener() {
    }

    public static void register(PackType type, PreparableReloadListener listener) {
        ResourceLoader.get(type).registerReloadListener(idFor(listener), listener);
    }

    private static Identifier idFor(PreparableReloadListener listener) {
        return Identifier.fromNamespaceAndPath("hollowengine", listener.getClass().getName().toLowerCase(Locale.ROOT).replace('$', '.'));
    }
}
