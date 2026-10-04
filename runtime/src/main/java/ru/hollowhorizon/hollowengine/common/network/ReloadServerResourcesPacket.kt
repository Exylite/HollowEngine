package ru.hollowhorizon.hollowengine.common.network

import ru.hollowhorizon.hollowengine.common.utils.compat.hasPermissions
import ru.hollowhorizon.hollowengine.common.utils.compat.server
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player
import ru.hollowhorizon.hollowengine.client.utils.lang
import ru.hollowhorizon.hollowengine.common.utils.literal

@HollowPacketHandler(HollowPacketHandler.Direction.TO_SERVER)
@Serializable
class ReloadServerResourcesPacket : HollowPacket {
    override fun handle(player: Player) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage("hollowengine.gui.ide.file.no_permissions_reload_server".lang.literal)
            return
        }
        val server = player.server ?: return
        server.commands.performPrefixedCommand(player.createCommandSourceStack(), "reload")
    }
}