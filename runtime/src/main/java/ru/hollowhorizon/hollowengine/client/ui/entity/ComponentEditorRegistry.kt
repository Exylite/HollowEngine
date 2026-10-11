package ru.hollowhorizon.hollowengine.client.ui.entity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.minecraft.resources.Identifier
import ru.hollowhorizon.hollowengine.client.ui.inspector.withField
import ru.hollowhorizon.hollowengine.common.attachments.api.Component

internal val LocalEntityEditorSession: ProvidableCompositionLocal<EntityEditorSession?> = compositionLocalOf { null }

class ComponentEditorScope internal constructor(
    private val entry: ComponentEntry,
    private val document: JsonObject,
    private val apply: (JsonObject) -> Unit,
) {
    val component: Component get() = entry.value
    val id: Identifier get() = entry.id
    val json: JsonObject get() = document

    fun set(field: String, value: JsonElement) = apply(document.withField(field, value))

    fun replace(document: JsonObject) = apply(document)
}

typealias ComponentEditorContent = @Composable (ComponentEditorScope) -> Unit

internal class ComponentEditorRegistration(
    val content: ComponentEditorContent,
    val replacesFields: Boolean,
    val before: Boolean,
)

/**
 * Extra UI for a specific component.
 */
object ComponentEditors {
    private val registry = LinkedHashMap<Identifier, ComponentEditorRegistration>()

    /**
     * @param before draws the extra UI above the generated fields instead of below them.
     * @param replacesFields hides the generated fields entirely.
     */
    fun register(
        id: Identifier,
        before: Boolean = false,
        replacesFields: Boolean = false,
        content: ComponentEditorContent,
    ) {
        registry[id] = ComponentEditorRegistration(content, replacesFields, before)
    }

    fun unregister(id: Identifier) {
        registry.remove(id)
    }

    internal fun of(id: Identifier): ComponentEditorRegistration? = registry[id]
}
