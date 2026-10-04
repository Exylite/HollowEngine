package ru.hollowhorizon.hollowengine.common.ui.hud

import net.minecraft.resources.Identifier
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which HUD layers the server has hidden per player, so the set survives a re-send and can
 * be restored after a respawn or dimension change without the caller tracking it.
 */
object ServerHudLayers {
    private val hidden = ConcurrentHashMap<UUID, Set<Identifier>>()

    operator fun get(player: UUID): Set<Identifier> = hidden[player].orEmpty()

    operator fun set(player: UUID, layers: Set<Identifier>) {
        if (layers.isEmpty()) hidden.remove(player) else hidden[player] = layers
    }

    fun clear(player: UUID) {
        hidden.remove(player)
    }
}
