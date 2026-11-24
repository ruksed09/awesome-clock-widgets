package com.versity.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeWidget
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.lockLook
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.home.components.widgetSize
import com.versity.launcher.ui.lock.LockStyleContent
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioSegmented
import com.versity.launcher.ui.studio.StudioSlider
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStore
import com.versity.launcher.widget.WidgetType
import kotlin.math.abs
import kotlin.math.roundToInt

private val SnapLinesY = listOf(0.2f, 1f / 3f, 0.5f, 2f / 3f)
private const val CLOCK = "clock"

/**
 * Full-screen editor over the real home or lock screen. Tap the clock or a widget to pick it (or use
 * the chips), drag anywhere to move the picked item and pinch or use the slider to resize it. Items
 * snap to the centre line and to thirds, with a guide and a light tick.
 */
@Composable
internal fun ArrangeEditor(
    settings: LauncherSettings,
    onDone: (ClockPlacement?, ClockPlacement?, List<HomeWidget>) -> Unit,
    onCancel: () -> Unit,
) {
    val c = Studio.colors
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val store = remember(context) { WidgetStore(context) }
    var target by rememberSaveable { mutableStateOf(ApplyTarget.HOME) }
    var home by remember { mutableStateOf(settings.clockPlacement) }
    var lock by remember { mutableStateOf(settings.lockClockPlacement) }
    var widgets by remember { mutableStateOf(settings.homeWidgets) }
    var picked by remember { mutableStateOf(CLOCK) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf<Float?>(null) }
    val specs = remember(widgets) { widgets.associate { it.id to (store.home(it.id) ?: WidgetSpec.default(WidgetType.DIGITAL)) } }
    // The lock screen has only the clock.
    val selected = if (target == ApplyTarget.LOCK || (picked != CLOCK && widgets.none { "w${it.id}" == picked })) CLOCK else picked

    fun placementOf(key: String): ClockPlacement? = when {
        key == CLOCK -> if (target == ApplyTarget.HOME) home else lock
        else -> widgets.firstOrNull { "w${it.id}" == key }?.placement
    }
    fun update(key: String, p: ClockPlacement?) {
        when {
            key == CLOCK && target == ApplyTarget.HOME -> home = p
            key == CLOCK -> lock = p
            p != null -> widgets = widgets.map { if ("w${it.id}" == key) it.copy(placement = p) else it }
        }
    }
    BackHandler(onBack = onCancel)

    Box(Modifier.fillMaxSize().testTag(TestTags.CLOCK_EDITOR).background(Color.Black)) {
        val now = remember { java.time.LocalDateTime.now() }
        if (target == ApplyTarget.HOME) {
            HomeContent(HomeUiState(settings = settings.copy(clockPlacement = home ?: ClockPlacement.DEFAULT, homeWidgets = widgets), now = now, isDefaultLauncher = true))
        } else {
            LockStyleContent(HomeUiState(settings = settings.lockLook().copy(clockPlacement = lock ?: ClockPlacement.DEFAULT), now = now, isDefaultLauncher = true), onUnlock = {})
        }
        // Gesture layer over the same area the screen lays out in, so fractions match. It owns every touch.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .pointerInput(target, widgets.map { it.id }) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        // Pick what is under the finger: widgets are drawn below the clock, so the clock wins.
                        if (target == ApplyTarget.HOME) {
                            val clock = (home ?: ClockPlacement.DEFAULT)
                            val clockW = w * 0.86f * clock.scale * if (settings.clockStyle == ClockStyle.ANALOGUE) 0.62f else 1f
                            val clockH = clockW * if (settings.clockStyle == ClockStyle.ANALOGUE) 1f else 0.45f
                            val inClock = abs(down.position.x - clock.x * w) < clockW / 2 && abs(down.position.y - clock.y * h) < clockH / 2
                            val hit = if (inClock) CLOCK else widgets.lastOrNull { hw ->
                                val (ww, wh) = widgetSize(hw.placement, specs.getValue(hw.id), w.roundToInt(), h.roundToInt())
                                abs(down.position.x - hw.placement.x * w) < ww / 2f && abs(down.position.y - hw.placement.y * h) < wh / 2f
                            }?.let { "w${it.id}" }
                            if (hit != null) picked = hit
                        }
                        val key = if (target == ApplyTarget.HOME) picked else CLOCK
                        var raw = placementOf(key) ?: ClockPlacement.DEFAULT
                        do {
                            val event = awaitPointerEvent()
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            raw = ClockPlacement(raw.x + pan.x / w, raw.y + pan.y / h, raw.scale * zoom).clamped()
                            val sx = abs(raw.x - 0.5f) < 0.025f
                            val sy = SnapLinesY.firstOrNull { abs(raw.y - it) < 0.018f }
                            if ((sx && !snapX) || (sy != null && sy != snapY)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            snapX = sx
                            snapY = sy
                            update(key, raw.copy(x = if (sx) 0.5f else raw.x, y = sy ?: raw.y))
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        snapX = false
                        snapY = null
                    }
                },
        ) {
            val guide = c.brand
            if (snapX) Box(Modifier.align(Alignment.TopCenter).width(1.dp).fillMaxHeight().background(guide))
            snapY?.let { y -> Box(Modifier.offset(y = maxHeight * y).fillMaxWidth().height(1.dp).background(guide)) }
            // Outline the picked widget, so it is clear what a drag will move.
            val density = LocalDensity.current
            widgets.firstOrNull { "w${it.id}" == selected }?.let { hw ->
                with(density) {
                    val sw = maxWidth.roundToPx()
                    val sh = maxHeight.roundToPx()
                    val (ww, wh) = widgetSize(hw.placement, specs.getValue(hw.id), sw, sh)
                    val x = (hw.placement.x * sw - ww / 2f).roundToInt().coerceIn(0, maxOf(0, sw - ww))
                    val y = (hw.placement.y * sh - wh / 2f).roundToInt().coerceIn(0, maxOf(0, sh - wh))
                    Box(
                        Modifier.offset { IntOffset(x, y) }.size(ww.toDp(), wh.toDp())
                            .border(1.5.dp, guide, RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
        // Top bar
        Row(
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 560.dp).fillMaxWidth().editorPanel(c).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioTextButton("Cancel", onClick = onCancel)
            Box(Modifier.weight(1f)) {
                StudioSegmented(listOf(ApplyTarget.HOME, ApplyTarget.LOCK), target, { if (it == ApplyTarget.HOME) "Home" else "Lock" }, { target = it })
            }
            StudioPrimaryButton("Done", Modifier.testTag(TestTags.EDITOR_DONE).padding(start = 4.dp)) { onDone(home, lock, widgets) }
        }
        // Bottom panel
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.systemBars).padding(12.dp)
                .widthIn(max = 560.dp).fillMaxWidth().editorPanel(c).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (target == ApplyTarget.HOME && widgets.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioChip("Clock", selected == CLOCK, "arrange_clock") { picked = CLOCK }
                    widgets.forEachIndexed { i, hw ->
                        val spec = specs.getValue(hw.id)
                        val same = widgets.take(i).count { specs.getValue(it.id).type == spec.type }
                        StudioChip(spec.type.title + if (same > 0) " ${same + 1}" else "", selected == "w${hw.id}", "arrange_w${hw.id}") { picked = "w${hw.id}" }
                    }
                }
            } else {
                StudioHint("Drag anywhere to move the clock. Pinch or use the slider to resize.")
            }
            val p = placementOf(selected) ?: ClockPlacement.DEFAULT
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioText("Size", StudioType.body, modifier = Modifier.width(44.dp))
                StudioSlider(
                    value = p.scale,
                    onChange = { update(selected, p.copy(scale = it).clamped()) },
                    range = ClockPlacement.MIN_SCALE..ClockPlacement.MAX_SCALE,
                    modifier = Modifier.weight(1f),
                    description = "Size, ${(p.scale * 100).roundToInt()} percent",
                )
                StudioText("${(p.scale * 100).roundToInt()}%", StudioType.small, c.muted, Modifier.width(48.dp).padding(start = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudioButton("Centre", glyph = Glyph.MOVE) { update(selected, p.copy(x = 0.5f)) }
                if (selected == CLOCK) {
                    StudioTextButton("Use layout position", Modifier.testTag(TestTags.EDITOR_RESET), color = c.brandText) { update(CLOCK, null) }
                } else {
                    StudioTextButton("Remove widget", color = c.danger) {
                        widgets = widgets.filterNot { "w${it.id}" == selected }
                        picked = CLOCK
                    }
                }
            }
        }
    }
}

/** Editor bars sit over a live screen, so they are opaque surfaces with a hairline, keeping controls legible. */
private fun Modifier.editorPanel(c: com.versity.launcher.ui.studio.StudioColors): Modifier =
    this.background(c.surface, StudioCardCorner).border(1.dp, c.line, StudioCardCorner)
