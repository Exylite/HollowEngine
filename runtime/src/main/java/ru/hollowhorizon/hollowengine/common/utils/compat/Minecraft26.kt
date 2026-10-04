package ru.hollowhorizon.hollowengine.common.utils.compat

import com.mojang.blaze3d.platform.Window
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.clock.WorldClocks
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import ru.hollowhorizon.hollowengine.client.render.legacy.MainTarget
import ru.hollowhorizon.hollowengine.client.render.legacy.RenderTarget

/*
 * Names the engine used on 1.21.1 that 26.x renamed or moved, kept as extensions so the call sites
 * read as before. A member of the same name always wins over an extension, so only things that no
 * longer exist as members are here.
 */

/** The screen is held by the in-game GUI now. */
val Minecraft.screen: Screen? get() = gui.screen()

fun Minecraft.setScreen(screen: Screen?) = gui.setScreen(screen)

/** The main target as the engine's own legacy [RenderTarget], see [MainTarget]. */
val Minecraft.mainRenderTarget: RenderTarget get() = MainTarget.get()

/** The GLFW handle, which used to be a public field. */
val Window.window: Long get() = handle()

val CompoundTag.allKeys: Set<String> get() = keySet()

fun ResourceKey<*>.location(): Identifier = identifier()

private fun levelPermission(level: Int): Permission? = when {
    level <= 0 -> null
    level == 1 -> Permissions.COMMANDS_MODERATOR
    level == 2 -> Permissions.COMMANDS_GAMEMASTER
    level == 3 -> Permissions.COMMANDS_ADMIN
    else -> Permissions.COMMANDS_OWNER
}

/** The old numeric operator levels: 1 moderator, 2 gamemaster, 3 admin, 4 owner. */
fun Player.hasPermissions(level: Int): Boolean =
    levelPermission(level)?.let { permissions().hasPermission(it) } ?: true

val ServerPlayer.server: MinecraftServer get() = level().server

val Entity.serverOrNull: MinecraftServer? get() = level().server

/** Time of day now lives in the overworld clock rather than in the level data. */
val Level.dayTime: Long get() = overworldClockTime

var ServerLevel.dayTime: Long
    get() = overworldClockTime
    set(value) {
        val clock = registryAccess().get(WorldClocks.OVERWORLD).orElse(null) ?: return
        clockManager().setTotalTicks(clock, value)
    }
