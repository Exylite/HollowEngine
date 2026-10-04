package ru.hollowhorizon.hollowengine.common.utils.compat

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.LevelBasedPermissionSet
import net.minecraft.server.permissions.PermissionLevel
import com.mojang.blaze3d.platform.Window
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.util.ARGB
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
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
import ru.hollowhorizon.hollowengine.client.render.legacy.LegacyRenderBuffers
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

/** The numeric operator levels the way the old `withPermission(int)` took them. */
fun CommandSourceStack.withPermission(level: Int): CommandSourceStack =
    withPermission(LevelBasedPermissionSet.forLevel(PermissionLevel.byId(level.coerceIn(0, 4))))

fun CommandSourceStack.hasPermission(level: Int): Boolean =
    levelPermission(level)?.let { permissions().hasPermission(it) } ?: true

val ServerPlayer.server: MinecraftServer get() = level().server

val Entity.server: MinecraftServer? get() = level().server

/** Time of day now lives in the overworld clock rather than in the level data. */
val Level.dayTime: Long get() = overworldClockTime

var ServerLevel.dayTime: Long
    get() = overworldClockTime
    set(value) {
        val clock = registryAccess().get(WorldClocks.OVERWORLD).orElse(null) ?: return
        clockManager().setTotalTicks(clock, value)
    }

/** Pixels in the engine's own packing: alpha, blue, green, red from the top, which is what 1.21 called RGBA. */
fun NativeImage.getPixelRGBA(x: Int, y: Int): Int = ARGB.toABGR(getPixel(x, y))

fun NativeImage.setPixelRGBA(x: Int, y: Int, abgr: Int) = setPixelABGR(x, y, abgr)

/** The image as a PNG, which vanilla now only writes to files. */
fun NativeImage.asByteArray(): ByteArray {
    val file = java.nio.file.Files.createTempFile("hollowengine", ".png")
    try {
        writeToFile(file)
        return java.nio.file.Files.readAllBytes(file)
    } finally {
        java.nio.file.Files.deleteIfExists(file)
    }
}

/** Entities and block entities save through [ValueOutput] now; the engine still keeps its own data as compounds. */
fun ValueOutput.putCompound(name: String, tag: CompoundTag) = store(name, CompoundTag.CODEC, tag)

fun ValueInput.getCompound(name: String): CompoundTag? = read(name, CompoundTag.CODEC).orElse(null)

/** The engine draws its own geometry by hand, so there is nothing of vanilla's to hand out here. */
fun Minecraft.renderBuffers(): LegacyRenderBuffers = LegacyRenderBuffers

/** The action bar message, or a chat line for `overlay = false`. */
fun Player.displayClientMessage(message: Component, overlay: Boolean) =
    if (overlay) sendOverlayMessage(message) else sendSystemMessage(message)
