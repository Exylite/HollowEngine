package ru.hollowhorizon.hollowengine.neoforge;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Locale;

final class ReloadListenerIds {
    private ReloadListenerIds() {
    }

    static Identifier idFor(PreparableReloadListener listener) {
        return Identifier.fromNamespaceAndPath("hollowengine", listener.getClass().getName().toLowerCase(Locale.ROOT).replace('$', '.'));
    }
}
