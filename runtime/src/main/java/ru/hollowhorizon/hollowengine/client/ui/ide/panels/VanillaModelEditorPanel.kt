package ru.hollowhorizon.hollowengine.client.ui.ide.panels

import androidx.compose.runtime.*
import kotlinx.coroutines.isActive
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import ru.hollowhorizon.hollowengine.client.handlers.TickHandler
import ru.hollowhorizon.hollowengine.client.ui.*
import ru.hollowhorizon.hollowengine.client.ui.layout.UiRect
import ru.hollowhorizon.hollowengine.client.ui.style.UiPaint
import ru.hollowhorizon.hollowengine.client.ui.widgets.tooltipOnHover
import ru.hollowhorizon.hollowengine.client.utils.lang

private const val GridIcon = "hollowengine:textures/gui/icons/graph.svg"
private const val AutoRotateIcon = "hollowengine:textures/gui/icons/reload.svg"
private const val ReloadIcon = "hollowengine:textures/gui/icons/load.svg"

/** Interactive preview for the standard Minecraft JSON model format. */
@Composable
internal fun VanillaModelEditorPanel(path: String) {
    val resource = remember(path) { path.toAssetResourceLocation() }
    val state = remember(resource) { VanillaModelViewerState(resource, Minecraft.getInstance().resourceManager) }

    Box(
        tags = listOf("model-editor-root", "vanilla-model-editor"),
        modifier = Modifier.style("hollowengine:ui/styles/model-editor.hss").size(100.percent, 100.percent),
    ) {
        VanillaModelPreview(state, modifier = Modifier.size(100.percent, 100.percent))
        Text(resource.toString(), tags = listOf("model-title"))
        Column(tags = listOf("model-toolbar")) {
            VanillaModelToggle(GridIcon, VanillaModelLang.REFERENCE_GRID.lang, state.showGrid) {
                state.showGrid = !state.showGrid
            }
            VanillaModelToggle(AutoRotateIcon, VanillaModelLang.AUTO_ROTATE.lang, state.autoRotate) {
                state.autoRotate = !state.autoRotate
            }
            VanillaModelToggle(
                ReloadIcon,
                VanillaModelLang.RELOAD.lang,
                active = false,
                onToggle = state::reload,
            )
        }
        state.error?.let { error ->
            Text(error, tags = listOf("vanilla-model-error"), modifier = Modifier.textWrap())
        }
    }
}

@Composable
private fun VanillaModelToggle(icon: String, tooltip: String, active: Boolean, onToggle: () -> Unit) {
    Box(
        tags = if (active) listOf("model-chip", "selected") else listOf("model-chip"),
        modifier = Modifier.cursor(UiCursorShape.HAND).onClick { event ->
            if (event.isLeftClick()) onToggle()
            event.consume()
        }.tooltipOnHover(tooltip),
    ) {
        Image(icon, tags = if (active) listOf("model-chip-icon", "selected") else listOf("model-chip-icon"))
    }
}

@Composable
private fun VanillaModelPreview(state: VanillaModelViewerState, modifier: Modifier = Modifier) {
    LaunchedEffect(state) {
        while (isActive) withFrameNanos(state.zoomSpring::advance)
    }
    Box(
        modifier = Modifier
            .input(hoverable = true, draggable = true)
            .cursor(UiCursorShape.HAND)
            .onDrag { event ->
                if (event.button == 1) {
                    val scale = state.zoom.coerceAtLeast(0.0001f)
                    state.offsetX += event.deltaX / scale
                    state.offsetY += event.deltaY / scale
                } else {
                    state.yaw = (state.yaw + event.deltaX / 3f) % 360f
                    state.pitch = (state.pitch + event.deltaY / 3f).coerceIn(-90f, 90f)
                }
                event.consume()
            }
            .onScroll { event ->
                state.zoomSpring.scroll(event.scrollY)
                event.consume()
            }
            .drawBehind(key = state) {
                if (state.showGrid) drawVanillaModelGrid(state)
                drawGl { state.render(rect, poseStack) }
            }
            .then(modifier),
    )
}

@Stable
private class VanillaModelViewerState(
    private val resource: Identifier,
    private val resourceManager: ResourceManager,
) {
    var yaw by mutableStateOf(35f)
    var pitch by mutableStateOf(25f)
    val zoomSpring = SpringZoom(0.72f, min = 0.1f, max = 10f, perNotch = ModelZoomPerNotch)
    val zoom: Float get() = zoomSpring.value
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var showGrid by mutableStateOf(true)
    var autoRotate by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
        private set

    init {
        reload()
    }

    /**
     * The preview needs vanilla's model baking, which 26.x rebuilt around block state models and quad
     * collections; until the engine drives that, the viewer checks the file and says what is missing.
     */
    fun reload() {
        error = when {
            !(resource.path.startsWith("models/") && resource.path.endsWith(".json")) ->
                VanillaModelLang.NOT_JSON_MODEL.lang(resource)

            resourceManager.getResource(resource).isEmpty -> VanillaModelLang.MISSING_MODEL.lang(resource)
            else -> VanillaModelLang.PREVIEW_UNAVAILABLE.lang
        }
    }

    fun render(rect: UiRect, stack: PoseStack) {
        if (autoRotate) yaw = (yaw + AutoRotateDegreesPerSecond * TickHandler.deltaFrameTime) % 360f
    }
}

private fun String.toAssetResourceLocation(): Identifier {
    val relative = substringAfter("assets/", missingDelimiterValue = "")
    require(relative.isNotEmpty() && '/' in relative) { VanillaModelLang.INVALID_ASSET_PATH.lang(this) }
    return Identifier.fromNamespaceAndPath(relative.substringBefore('/'), relative.substringAfter('/'))
}

private fun UiCanvasDrawScope.drawVanillaModelGrid(state: VanillaModelViewerState) {
    val spacing = (GridSpacing * state.zoom).coerceIn(8f, 512f)
    val paint = UiPaint.Color(GridColor)
    var x = (state.offsetX * state.zoom + size.width / 2f) % spacing
    if (x < 0f) x += spacing
    while (x < size.width) {
        drawRect(UiRect(x, 0f, 1f, size.height), paint)
        x += spacing
    }
    var y = (state.offsetY * state.zoom + size.height / 2f) % spacing
    if (y < 0f) y += spacing
    while (y < size.height) {
        drawRect(UiRect(0f, y, size.width, 1f), paint)
        y += spacing
    }
}

private const val GridSpacing = 36f
private const val AutoRotateDegreesPerSecond = 20f
private const val MinimumOrthographicDepth = 1000f
private val GridColor = UiColor(0.62f, 0.7f, 0.85f, 0.14f)

private object VanillaModelLang {
    private const val ROOT = "hollowengine.gui.ide.vanilla_model."

    const val REFERENCE_GRID = ROOT + "reference_grid"
    const val AUTO_ROTATE = ROOT + "auto_rotate"
    const val RELOAD = ROOT + "reload"
    const val BUILTIN_RENDERER = ROOT + "error.builtin_renderer"
    const val LOAD_FAILED = ROOT + "error.load_failed"
    const val NOT_JSON_MODEL = ROOT + "error.not_json_model"
    const val EMPTY_BAKED_MODEL = ROOT + "error.empty_baked_model"
    const val MISSING_MODEL = ROOT + "error.missing_model"
    const val PREVIEW_UNAVAILABLE = ROOT + "error.preview_unavailable"
    const val INVALID_ASSET_PATH = ROOT + "error.invalid_asset_path"
}
