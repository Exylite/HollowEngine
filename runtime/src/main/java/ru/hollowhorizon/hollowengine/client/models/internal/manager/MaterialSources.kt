package ru.hollowhorizon.hollowengine.client.models.internal.manager

import ru.hollowhorizon.hollowengine.client.render.legacy.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.PlayerModelType
import ru.hollowhorizon.hollowengine.common.attachments.components.PlayerArms
import ru.hollowhorizon.hollowengine.common.events.ClientOnly
import ru.hollowhorizon.hollowengine.common.models.MaterialSource
import ru.hollowhorizon.hollowengine.common.models.PlayerSkinPart
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/** A material source turned into something the renderer can bind. */
data class ResolvedMaterial(
    val texture: Identifier?,
    val normal: Identifier? = null,
    val specular: Identifier? = null,
    val color: String? = null,
)

/**
 * Turns a [MaterialSource] into textures.
 */
@ClientOnly
object MaterialSources {
    private val lookups = ConcurrentHashMap<String, CompletableFuture<PlayerSkin?>>()

    /**
     * Resolves any [source] as Material.
     */
    fun resolve(source: MaterialSource, onLoaded: () -> Unit = {}): ResolvedMaterial = when (source) {
        is MaterialSource.Texture -> ResolvedMaterial(
            texture = source.texture,
            normal = source.normal,
            specular = source.specular,
            color = source.color,
        )

        is MaterialSource.Player -> skinOf(source.player, onLoaded).let { skin ->
            ResolvedMaterial(
                texture = when (source.part) {
                    PlayerSkinPart.SKIN -> skin.body().texturePath()
                    PlayerSkinPart.CAPE -> skin.cape()?.texturePath()
                    PlayerSkinPart.ELYTRA -> skin.elytra()?.texturePath()
                },
            )
        }
    }

    /** The arm shape [source] implies, for a skin that came off a real player, and null otherwise. */
    fun armsOf(source: MaterialSource): PlayerArms? = when (source) {
        is MaterialSource.Texture -> null
        is MaterialSource.Player -> when (skinOf(source.player) {}.model()) {
            PlayerModelType.SLIM -> PlayerArms.SLIM
            PlayerModelType.WIDE -> PlayerArms.WIDE
        }
    }

    /**
     * The skin of a player named by nickname or uuid.
     *
     * Someone on this server is known already: the client got their skin with the player list.
     * Anyone else loading using profile lookup.
     */
    private fun skinOf(player: String, onLoaded: () -> Unit): PlayerSkin {
        val uuid = player.asUuid()
        val connection = Minecraft.getInstance().connection
        val online = uuid?.let { connection?.getPlayerInfo(it) } ?: connection?.getPlayerInfo(player)
        if (online != null) return online.skin

        val lookup = lookups.computeIfAbsent(player) { lookUp(it, uuid) }
        if (!lookup.isDone) lookup.thenRun { RenderSystem.recordRenderCall(onLoaded) }
        return lookup.getNow(null) ?: DefaultPlayerSkin.get(uuid ?: offlineUuid(player))
    }

    private fun lookUp(player: String, uuid: UUID?): CompletableFuture<PlayerSkin?> {
        // the resolver asks Mojang's servers and answers from its own cache, so it runs off the render thread
        return CompletableFuture.supplyAsync {
            val resolver = Minecraft.getInstance().services().profileResolver()
            (uuid?.let(resolver::fetchById) ?: resolver.fetchByName(player)).orElse(null)
        }.thenCompose { found ->
            if (found == null) CompletableFuture.completedFuture<PlayerSkin?>(null)
            else Minecraft.getInstance().skinManager.get(found).thenApply { it.orElse(null) }
        }.exceptionally { null }
    }

    private fun String.asUuid(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private fun offlineUuid(name: String): UUID =
        UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8))
}
