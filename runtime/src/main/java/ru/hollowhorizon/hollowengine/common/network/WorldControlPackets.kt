package ru.hollowhorizon.hollowengine.common.network

import ru.hollowhorizon.hollowengine.common.utils.compat.dayTime
import ru.hollowhorizon.hollowengine.common.utils.compat.hasPermissions
import ru.hollowhorizon.hollowengine.common.utils.compat.server
import kotlinx.serialization.Serializable
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.GameType
import ru.hollowhorizon.hollowengine.client.ui.ide.WorldControlClient
import ru.hollowhorizon.hollowengine.common.utils.PlayerPermissions

@Serializable
enum class WorldWeather { CLEAR, RAIN, THUNDER }

@Serializable
enum class WorldRule {
    DAYLIGHT_CYCLE,
    WEATHER_CYCLE,
    MOB_SPAWNING,
    MOB_GRIEFING,
    KEEP_INVENTORY,
    FIRE_TICK;

    /** The translation key vanilla gives the rule behind this switch. */
    val descriptionId: String
        get() = when (this) {
            DAYLIGHT_CYCLE -> GameRules.ADVANCE_TIME.descriptionId
            WEATHER_CYCLE -> GameRules.ADVANCE_WEATHER.descriptionId
            MOB_SPAWNING -> GameRules.SPAWN_MOBS.descriptionId
            MOB_GRIEFING -> GameRules.MOB_GRIEFING.descriptionId
            KEEP_INVENTORY -> GameRules.KEEP_INVENTORY.descriptionId
            FIRE_TICK -> GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER.descriptionId
        }

    fun get(rules: GameRules): Boolean = when (this) {
        DAYLIGHT_CYCLE -> rules.get(GameRules.ADVANCE_TIME)
        WEATHER_CYCLE -> rules.get(GameRules.ADVANCE_WEATHER)
        MOB_SPAWNING -> rules.get(GameRules.SPAWN_MOBS)
        MOB_GRIEFING -> rules.get(GameRules.MOB_GRIEFING)
        KEEP_INVENTORY -> rules.get(GameRules.KEEP_INVENTORY)
        // fire has no switch of its own any more, only how far from a player it spreads
        FIRE_TICK -> rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER) != 0
    }

    fun set(rules: GameRules, value: Boolean, server: MinecraftServer) {
        when (this) {
            DAYLIGHT_CYCLE -> rules.set(GameRules.ADVANCE_TIME, value, server)
            WEATHER_CYCLE -> rules.set(GameRules.ADVANCE_WEATHER, value, server)
            MOB_SPAWNING -> rules.set(GameRules.SPAWN_MOBS, value, server)
            MOB_GRIEFING -> rules.set(GameRules.MOB_GRIEFING, value, server)
            KEEP_INVENTORY -> rules.set(GameRules.KEEP_INVENTORY, value, server)
            FIRE_TICK -> rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, if (value) FIRE_SPREAD_DEFAULT else 0, server)
        }
    }

    private companion object {
        const val FIRE_SPREAD_DEFAULT = 128
    }
}

@Serializable
data class WorldControlState(
    val dayTime: Long,
    val weather: WorldWeather,
    val difficulty: Int,
    val difficultyLocked: Boolean,
    val gameMode: Int,
    val rules: Map<WorldRule, Boolean>,
)

@Serializable
data class WorldChange(
    val timeOfDay: Long? = null,
    val weather: WorldWeather? = null,
    val difficulty: Int? = null,
    val gameMode: Int? = null,
    val rule: WorldRule? = null,
    val ruleValue: Boolean = false,
)

@HollowPacketHandler(HollowPacketHandler.Direction.TO_SERVER)
@Serializable
class RequestWorldControlPacket(val change: WorldChange? = null) : HollowPacket {
    override fun handle(player: Player) {
        val serverPlayer = player as? ServerPlayer ?: return
        if (!player.hasPermissions(PlayerPermissions.GAMEMASTER)) {
            WorldControlStatePacket(error = WorldControlErrors.OPERATOR_REQUIRED).send(serverPlayer)
            return
        }
        change?.let { WorldControl.apply(serverPlayer, it) }
        WorldControlStatePacket(WorldControl.snapshot(serverPlayer)).send(serverPlayer)
    }
}

@HollowPacketHandler(HollowPacketHandler.Direction.TO_CLIENT)
@Serializable
class WorldControlStatePacket(
    val state: WorldControlState? = null,
    val error: String? = null,
) : HollowPacket {
    override fun handle(player: Player) {
        WorldControlClient.accept(this)
    }
}

object WorldControlErrors {
    const val OPERATOR_REQUIRED = "hollowengine.gui.ide.world.error.operator_required"
}

internal object WorldControl {
    private const val DAY_TICKS = 24_000L

    fun snapshot(player: ServerPlayer): WorldControlState {
        val server = player.server
        val overworld = server.overworld()
        val weather = overworld.weatherData
        return WorldControlState(
            dayTime = overworld.dayTime,
            weather = when {
                weather.isThundering -> WorldWeather.THUNDER
                weather.isRaining -> WorldWeather.RAIN
                else -> WorldWeather.CLEAR
            },
            difficulty = server.worldData.difficulty.id,
            difficultyLocked = server.worldData.isDifficultyLocked,
            gameMode = player.gameMode.gameModeForPlayer.id,
            rules = WorldRule.entries.associateWith { it.get(server.gameRules) },
        )
    }

    fun apply(player: ServerPlayer, change: WorldChange) {
        val server = player.server
        change.timeOfDay?.let { setTimeOfDay(server, it) }
        change.weather?.let { setWeather(server.overworld(), it) }
        change.difficulty?.let { id ->
            if (!server.worldData.isDifficultyLocked) server.setDifficulty(Difficulty.byId(id), true)
        }
        change.gameMode?.let { id -> player.setGameMode(GameType.byId(id)) }
        change.rule?.let { rule -> rule.set(server.gameRules, change.ruleValue, server) }
    }

    /** Moves the overworld clock to [timeOfDay] within the current day, so the day counter and moon phase stay. */
    fun setTimeOfDay(server: MinecraftServer, timeOfDay: Long) {
        val target = timeOfDay.mod(DAY_TICKS)
        val overworld = server.overworld()
        // the clock tells every player by itself
        overworld.dayTime = overworld.dayTime - overworld.dayTime.mod(DAY_TICKS) + target
    }

    private fun setWeather(level: ServerLevel, weather: WorldWeather) {
        val random = level.random
        when (weather) {
            WorldWeather.CLEAR -> setWeatherParameters(level, ServerLevel.RAIN_DELAY.sample(random), 0, false, false)
            WorldWeather.RAIN -> setWeatherParameters(level, 0, ServerLevel.RAIN_DURATION.sample(random), true, false)
            WorldWeather.THUNDER ->
                setWeatherParameters(level, 0, ServerLevel.THUNDER_DURATION.sample(random), true, true)
        }
    }

    private fun setWeatherParameters(level: ServerLevel, clearTime: Int, weatherTime: Int, raining: Boolean, thundering: Boolean) {
        val data = level.weatherData
        data.clearWeatherTime = clearTime
        data.rainTime = weatherTime
        data.thunderTime = weatherTime
        data.isRaining = raining
        data.isThundering = thundering
    }
}
