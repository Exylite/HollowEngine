package ru.hollowhorizon.hollowengine.common.ui

import net.minecraft.resources.Identifier
import ru.hollowhorizon.hollowengine.HollowEngine
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything `.ui.kts` scripts have declared. Scripts are recompiled on reload, so the registry is
 * rebuilt from scratch each time rather than merged, a renamed or deleted screen must not linger.
 */
object UiDefinitionRegistry {
    private val screens = ConcurrentHashMap<Identifier, UiScreenDefinition>()
    private val overlays = ConcurrentHashMap<Identifier, UiOverlayDefinition>()
    private val surfaces = ConcurrentHashMap<Identifier, UiSurfaceDefinition>()

    /** Replaced screen class name -> the scripted screen that replaces it. */
    private val overrides = ConcurrentHashMap<String, OverrideEntry>()

    private class OverrideEntry(val definition: UiScreenDefinition, val includeSubclasses: Boolean)

    val allScreens: Collection<UiScreenDefinition> get() = screens.values
    val allOverlays: Collection<UiOverlayDefinition> get() = overlays.values

    val hasScreenOverrides: Boolean get() = overrides.isNotEmpty()

    fun register(definition: UiScreenDefinition) {
        screens[definition.id] = definition
        definition.overrides.forEach { override ->
            val previous = overrides.put(override.className, OverrideEntry(definition, override.includeSubclasses))
            if (previous != null && previous.definition.id != definition.id) {
                HollowEngine.LOGGER.warn(
                    "UI screen {} overrides {}, declared in {}",
                    definition.id, override.className, previous.definition.id,
                )
            }
        }
    }

    fun register(definition: UiOverlayDefinition) {
        overlays[definition.id] = definition
    }

    fun register(definition: UiSurfaceDefinition) {
        surfaces[definition.id] = definition
    }

    fun screen(id: Identifier): UiScreenDefinition? = screens[id]

    fun overlay(id: Identifier): UiOverlayDefinition? = overlays[id]

    fun surface(id: Identifier): UiSurfaceDefinition? = surfaces[id]

    fun screenOverride(type: Class<*>): UiScreenDefinition? {
        if (overrides.isEmpty()) return null
        var current: Class<*>? = type
        var depth = 0
        while (current != null && current != Any::class.java) {
            val entry = overrides[current.name]
            if (entry != null && (depth == 0 || entry.includeSubclasses)) return entry.definition
            current = current.superclass
            depth++
        }
        return null
    }

    fun clear() {
        screens.clear()
        overlays.clear()
        surfaces.clear()
        overrides.clear()
    }
}
