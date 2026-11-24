package com.versity.launcher.ui.home.components

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.viewinterop.AndroidView
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.HomeWidget
import com.versity.launcher.ui.TestTags
import com.versity.launcher.widget.WidgetRenderer
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStore
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
import com.versity.launcher.widget.Widgets
import kotlin.math.max
import kotlin.math.roundToInt

/** Bumped whenever a home widget's look is saved, so the home screen redraws it. */
object HomeWidgetLooks {
    val version = mutableIntStateOf(0)
    fun changed() { version.intValue++ }
}

/**
 * A studio widget drawn from the same RemoteViews the phone's home screen would get, so a widget looks
 * identical in the studio, on Versity's home screen and on any other launcher. TextClock and AnalogClock
 * inside keep time on their own.
 */
@Composable
fun WidgetView(
    spec: WidgetSpec,
    modifier: Modifier = Modifier,
    clickable: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    /** Rebuilds the widget when it changes; countdowns pass the minute so their day count stays right. */
    refresh: Any? = null,
    /** The spinner follows a finger here (the studio preview turns it without opening apps). */
    spinnable: Boolean = clickable,
    /** Where a spinner came to rest, to remember it. */
    onTurn: ((Int) -> Unit)? = null,
) {
    if (spec.style == WidgetStyle.RADIAL_SPINNER) {
        SpinnerView(spec, modifier, spinnable = spinnable, openApps = clickable, onLongPress = onLongPress, onTurn = onTurn)
        return
    }
    val longPress by rememberUpdatedState(onLongPress)
    BoxWithConstraints(modifier) {
        val w = maxWidth.value.roundToInt().coerceAtLeast(1)
        val h = maxHeight.value.roundToInt().coerceAtLeast(1)
        key(spec, w, h, refresh) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        val views = WidgetRenderer.build(ctx, spec, w, h, interactive = clickable)
                        if (clickable && !spec.type.collection) views.setOnClickPendingIntent(com.versity.launcher.R.id.w_root, Widgets.clickIntent(ctx, spec.type))
                        val child = runCatching { views.apply(ctx, this) }.getOrNull()
                        if (child != null) {
                            if (longPress != null) {
                                child.isLongClickable = true
                                child.setOnLongClickListener { longPress?.invoke(); true }
                            }
                            if (!clickable) child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                            addView(child, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Width of a home widget as a share of the screen width, before its placement scale. */
const val HOME_WIDGET_BASE = 0.6f

/** A sensible starting size for a new home widget: wide ones span most of the width, square ones half. */
fun newWidgetPlacement(spec: WidgetSpec, y: Float = 0.56f): ClockPlacement =
    ClockPlacement(0.5f, y, if (spec.aspect >= 1.6f) 1.4f else 0.8f).clamped()

/**
 * The widgets on Versity's home screen, each centred on its placement and kept fully on screen.
 * Long-pressing one offers to edit, move or remove it.
 */
@Composable
fun HomeWidgetsLayer(
    widgets: List<HomeWidget>,
    modifier: Modifier = Modifier,
    minute: Any? = null,
    onLongPress: (HomeWidget) -> Unit = {},
) {
    if (widgets.isEmpty()) return
    val context = LocalContext.current
    val store = remember(context) { WidgetStore(context) }
    val version = HomeWidgetLooks.version.intValue
    val specs = remember(widgets, version) { widgets.map { it to (store.home(it.id) ?: WidgetSpec.default(WidgetType.DIGITAL)) } }
    Layout(
        content = {
            specs.forEach { (w, spec) ->
                key(w.id) {
                    WidgetView(
                        spec,
                        Modifier.testTag(TestTags.homeWidget(w.id)).semantics { contentDescription = spec.type.title },
                        clickable = true,
                        onLongPress = { onLongPress(w) },
                        refresh = minute.takeIf { spec.type == WidgetType.COUNTDOWN },
                        onTurn = { turn -> store.saveHome(w.id, spec.copy(turn = turn)) },
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val sw = constraints.maxWidth
        val sh = constraints.maxHeight
        val placeables = measurables.mapIndexed { i, m ->
            val (w, spec) = specs[i]
            val (width, height) = widgetSize(w.placement, spec, sw, sh)
            m.measure(Constraints.fixed(width, height))
        }
        layout(sw, sh) {
            placeables.forEachIndexed { i, p ->
                val pl = specs[i].first.placement
                val x = (pl.x * sw - p.width / 2f).roundToInt().coerceIn(0, max(0, sw - p.width))
                val y = (pl.y * sh - p.height / 2f).roundToInt().coerceIn(0, max(0, sh - p.height))
                p.place(x, y)
            }
        }
    }
}

/** Pixel size of a home widget on a [sw] x [sh] screen: width from the placement, height from the style. */
fun widgetSize(placement: ClockPlacement, spec: WidgetSpec, sw: Int, sh: Int): Pair<Int, Int> {
    var width = (sw * HOME_WIDGET_BASE * placement.scale).roundToInt().coerceIn(1, sw)
    var height = (width / spec.aspect).roundToInt()
    val maxH = (sh * 0.9f).roundToInt()
    if (height > maxH) {
        height = maxH
        width = (height * spec.aspect).roundToInt().coerceAtMost(sw)
    }
    return width to height.coerceAtLeast(1)
}
