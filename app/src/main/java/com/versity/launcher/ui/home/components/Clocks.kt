package com.versity.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.LiningFigures
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun hourText(time: LocalTime, is24Hour: Boolean): String {
    val h = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
    return "%02d".format(h)
}

internal fun minuteText(time: LocalTime) = "%02d".format(time.minute)

/** Hours over minutes, light weight. Size follows the available width, within sane bounds. */
@Composable
fun StackedClock(
    time: LocalTime,
    is24Hour: Boolean,
    modifier: Modifier = Modifier,
    sizeRange: ClosedFloatingPointRange<Float> = 48f..80f,
    centered: Boolean = false,
) {
    BoxWithConstraints(modifier.testTag(TestTags.CLOCK_STACKED), contentAlignment = if (centered) Alignment.Center else Alignment.TopStart) {
        val size = (maxWidth.value * 0.26f).coerceIn(sizeRange.start, sizeRange.endInclusive).sp
        val style = TextStyle(
            color = LauncherTheme.colors.onSurface,
            fontFamily = LauncherTheme.font,
            fontWeight = FontWeight.Light,
            fontSize = size,
            lineHeight = size * 0.95f,
            fontFeatureSettings = LiningFigures,
        )
        Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text(hourText(time, is24Hour), style = style)
            Text(minuteText(time), style = style.copy(color = LauncherTheme.colors.accent))
        }
    }
}

@Composable
fun SingleLineClock(
    time: LocalTime,
    is24Hour: Boolean,
    modifier: Modifier = Modifier,
    sizeRange: ClosedFloatingPointRange<Float> = 40f..68f,
    centered: Boolean = false,
    /** Share of the width that sets the font size. */
    widthFactor: Float = 0.2f,
    weight: FontWeight = FontWeight.Light,
    /** Silver vertical gradient on the digits, used by the Orbit layout and lock-style screen. */
    metallic: Boolean = false,
) {
    BoxWithConstraints(modifier.testTag(TestTags.CLOCK_SINGLE_LINE), contentAlignment = if (centered) Alignment.Center else Alignment.TopStart) {
        val size = (maxWidth.value * widthFactor).coerceIn(sizeRange.start, sizeRange.endInclusive).sp
        val colors = LauncherTheme.colors
        val base = TextStyle(fontFamily = LauncherTheme.font, fontWeight = weight, fontSize = size, lineHeight = size * 1.05f, fontFeatureSettings = LiningFigures)
        Text(
            text = "${hourText(time, is24Hour)}:${minuteText(time)}",
            maxLines = 1,
            style = if (metallic) base.copy(brush = Brush.verticalGradient(listOf(colors.onSurface, colors.muted)))
            else base.copy(color = colors.onSurface),
        )
    }
}

/** Thin-line analogue face: 12 ticks, hour and minute hands. No seconds hand, so no per-second redraw. */
@Composable
private fun AnalogueFace(time: LocalTime, modifier: Modifier = Modifier, dense: Boolean = false) {
    val colors = LauncherTheme.colors
    Canvas(modifier.aspectRatio(1f)) {
        val c = center
        val r = min(size.width, size.height) / 2f - 1.dp.toPx()
        if (dense) drawCircle(Color.Black.copy(alpha = 0.72f), radius = r)
        drawCircle(colors.border, radius = r, style = Stroke(1.dp.toPx()))
        val ticks = if (dense) 60 else 12
        repeat(ticks) { i ->
            val a = Math.toRadians(i * (360.0 / ticks) - 90.0)
            val major = if (dense) i % 5 == 0 else i % 3 == 0
            val len = if (major) 0.14f else 0.07f
            drawLine(
                color = if (major) colors.onSurface else colors.muted,
                start = c + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * (0.9f - len)),
                end = c + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * 0.9f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        fun hand(angleDeg: Double, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            val a = Math.toRadians(angleDeg - 90.0)
            drawLine(
                color, c, c + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * length),
                strokeWidth = width, cap = StrokeCap.Butt,
            )
        }
        val minuteDeg = time.minute * 6.0 + time.second * 0.1
        val hourDeg = (time.hour % 12) * 30.0 + time.minute * 0.5
        hand(hourDeg, 0.5f, 2.dp.toPx(), colors.onSurface)
        hand(minuteDeg, 0.75f, 1.5.dp.toPx(), colors.accent)
        drawCircle(colors.accent, radius = 2.5.dp.toPx(), center = c)
    }
}

/** Large analogue clock for the ANALOGUE clock style. */
@Composable
fun BigAnalogueClock(time: LocalTime, modifier: Modifier = Modifier) {
    AnalogueFace(time, modifier.testTag(TestTags.CLOCK_ANALOGUE))
}

/** The fixed analogue widget. Tagged separately so it never counts as the chosen clock style. */
@Composable
fun AnalogueClockWidget(time: LocalTime, modifier: Modifier = Modifier, dense: Boolean = false) {
    AnalogueFace(time, modifier.testTag(TestTags.WIDGET_ANALOGUE), dense)
}
