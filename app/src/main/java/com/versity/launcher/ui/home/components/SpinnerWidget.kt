package com.versity.launcher.ui.home.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.versity.launcher.widget.CollectionArt
import com.versity.launcher.widget.CollectionArt.SPIN_SLOTS
import com.versity.launcher.widget.CollectionArt.SPIN_STEP
import com.versity.launcher.widget.WidgetSpec
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The spinner launcher, live: drag the wheel round with a finger, flick it and it glides to a stop on
 * an app, with a light tick as each app passes the pointer. Tapping an app opens it ([openApps]);
 * tapping the hub turns the wheel one step. It is drawn by the same code as the widget image, so it
 * looks exactly like the spinner on any other home screen.
 *
 * [spinnable] off makes it a still picture (style tiles, lists). [onTurn] receives the step the
 * wheel came to rest on, so it can be remembered.
 */
@Composable
fun SpinnerView(
    spec: WidgetSpec,
    modifier: Modifier = Modifier,
    spinnable: Boolean = false,
    openApps: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onTurn: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val angle = remember { Animatable(spec.turn * SPIN_STEP) }
    val turned by rememberUpdatedState(onTurn)
    val longPress by rememberUpdatedState(onLongPress)
    val apps = spec.apps.take(SPIN_SLOTS)
    // Icons load off the main thread; the wheel draws empty circles until they arrive.
    val icons by produceState(emptyMap<String, Drawable?>(), apps) {
        value = withContext(Dispatchers.IO) { apps.filter { it.isNotBlank() }.associateWith { CollectionArt.icon(context, it) } }
    }
    val labels = remember(apps) { apps.map { it.takeIf(String::isNotBlank)?.let { k -> CollectionArt.label(context, k) } } }

    // A turn saved elsewhere (another device size, the studio) moves the wheel there.
    LaunchedEffect(spec.turn) {
        val target = nearest(angle.value, spec.turn)
        if (!angle.isRunning && abs(target - angle.value) > 0.5f) angle.animateTo(target, Settle)
    }
    // A light tick each time an app passes the pointer.
    if (spinnable) {
        LaunchedEffect(Unit) {
            var last = slotAt(angle.value)
            snapshotFlow { slotAt(angle.value) }.collect { slot ->
                if (slot != last) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                last = slot
            }
        }
    }

    fun settle(target: Float, velocity: Float = 0f) {
        scope.launch {
            angle.animateTo(target, Settle, initialVelocity = velocity)
            turned?.invoke(stepOf(target))
        }
    }
    fun step(by: Int) = settle(snap(angle.targetValue) + by * SPIN_STEP)

    val description = labels.getOrNull(stepOf(angle.value).let { (SPIN_SLOTS - it).mod(SPIN_SLOTS) })
        ?.let { "App spinner, $it at the top" } ?: "App spinner"
    Canvas(
        modifier
            .semantics {
                contentDescription = description
                if (spinnable) {
                    customActions = buildList {
                        add(CustomAccessibilityAction("Turn clockwise") { step(1); true })
                        add(CustomAccessibilityAction("Turn anticlockwise") { step(-1); true })
                        if (openApps) labels.forEachIndexed { i, label ->
                            if (label != null) add(CustomAccessibilityAction("Open $label") { open(context, apps[i]); true })
                        }
                    }
                }
            }
            .then(
                if (!spinnable) Modifier
                else Modifier
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Past the touch slop it is a turn, not a tap.
                            val first = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
                            scope.launch { angle.stop() }
                            // The finger's angle is tracked here and pushed to the wheel, so quick moves add up
                            // exactly; samples carry the touch events' own times, for the flick.
                            var dragAngle = angle.value
                            val samples = ArrayDeque<Pair<Long, Float>>()
                            fun follow(change: PointerInputChange) {
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val now = change.position - c
                                val before = change.previousPosition - c
                                // Turning near the very centre would jump wildly, so it is ignored there.
                                if (hypot(now.x, now.y) > minOf(size.width, size.height) * 0.08f) {
                                    var d = Math.toDegrees((atan2(now.y, now.x) - atan2(before.y, before.x)).toDouble()).toFloat()
                                    if (d > 180f) d -= 360f
                                    if (d < -180f) d += 360f
                                    dragAngle += d
                                    val to = dragAngle
                                    scope.launch { angle.snapTo(to) }
                                }
                                samples.addLast(change.uptimeMillis to dragAngle)
                                while (samples.size > 8) samples.removeFirst()
                                change.consume()
                            }
                            follow(first)
                            val lifted = drag(first.id) { follow(it) }
                            val end = currentEvent.changes.firstOrNull()?.uptimeMillis ?: samples.lastOrNull()?.first ?: 0L
                            val v = if (lifted) velocity(samples, end) else 0f
                            // Carry on a little in the flick's direction, then rest on the nearest app.
                            settle(snap(dragAngle + v * 0.22f), v)
                        }
                    }
                    .pointerInput(openApps, spec) {
                        detectTapGestures(
                            onLongPress = longPress?.let { { _: Offset -> it() } },
                        ) { pos ->
                            val s = minOf(size.width, size.height).toFloat()
                            val g = CollectionArt.SpinnerGeometry(s, spec.spacing)
                            val x = pos.x - (size.width - s) / 2f - g.c
                            val y = pos.y - (size.height - s) / 2f - g.c
                            if (hypot(x, y) <= g.hub * 1.15f) {
                                step(1)
                                return@detectTapGestures
                            }
                            if (!openApps) return@detectTapGestures
                            val hit = (0 until SPIN_SLOTS).firstOrNull { i ->
                                val a = Math.toRadians(CollectionArt.spinAngle(i, angle.value).toDouble())
                                hypot(x - g.track * sin(a).toFloat(), y + g.track * cos(a).toFloat()) <= g.lobe * 1.2f
                            }
                            hit?.let { apps.getOrNull(it) }?.takeIf { it.isNotBlank() }?.let { open(context, it) }
                        }
                    },
            ),
    ) {
        val s = size.minDimension
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate((size.width - s) / 2f, (size.height - s) / 2f)
            CollectionArt.drawSpinner(native, spec, s, angle.value) { icons[it] }
            native.restore()
        }
    }
}

private val Settle = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)

/** The resting angle nearest [a]. */
private fun snap(a: Float) = (a / SPIN_STEP).roundToInt() * SPIN_STEP

/** The step (0..7) a resting angle stands for. */
private fun stepOf(a: Float) = (a / SPIN_STEP).roundToInt().mod(SPIN_SLOTS)

/** Which app is nearest the pointer, counted in steps, for the haptic tick. */
private fun slotAt(a: Float) = floor((a + SPIN_STEP / 2f) / SPIN_STEP).toInt()

/** The angle for [turn] closest to [from], so a saved turn never spins the wheel the long way round. */
private fun nearest(from: Float, turn: Int): Float {
    val base = turn * SPIN_STEP
    val k = ((from - base) / 360f).roundToInt()
    return base + k * 360f
}

/**
 * Degrees per second over the last tenth of a second before the finger lifted at [end], capped so a
 * wild flick stays controllable. A finger that stopped before lifting gives no flick.
 */
private fun velocity(samples: ArrayDeque<Pair<Long, Float>>, end: Long): Float {
    val (t1, a1) = samples.lastOrNull() ?: return 0f
    if (end - t1 > 80) return 0f
    val (t0, a0) = samples.firstOrNull { t1 - it.first <= 100 } ?: return 0f
    if (t1 <= t0) return 0f
    return ((a1 - a0) / ((t1 - t0) / 1000f)).coerceIn(-2400f, 2400f)
}

private fun open(context: android.content.Context, key: String) {
    CollectionArt.launchIntent(context, key)?.let { runCatching { context.startActivity(it) } }
}
