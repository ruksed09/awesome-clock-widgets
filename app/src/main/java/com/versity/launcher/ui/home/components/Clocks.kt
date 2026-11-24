package com.versity.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun hourText(time: LocalTime, is24Hour: Boolean): String {
    val h = if (is24Hour) time.hour else (time.hour + 11) % 12 + 1
    return "%02d".format(h)
}

internal fun minuteText(time: LocalTime) = "%02d".format(time.minute)

/**
 * Large analogue clock for the ANALOGUE style: sixty fine ticks with twelve stronger hour marks, a
 * solid hour hand, a slimmer minute hand in the accent and a small pivot. No seconds hand, so it only
 * redraws once a minute. Everything scales with the size it is given.
 */
@Composable
fun BigAnalogueClock(time: LocalTime, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    Canvas(modifier.testTag(TestTags.CLOCK_ANALOGUE).aspectRatio(1f)) {
        val c = center
        val r = min(size.width, size.height) / 2f
        val unit = r / 100f
        fun dir(deg: Double): Offset {
            val a = Math.toRadians(deg - 90.0)
            return Offset(cos(a).toFloat(), sin(a).toFloat())
        }
        repeat(60) { i ->
            val major = i % 5 == 0
            val d = dir(i * 6.0)
            val outer = r * 0.98f
            val inner = r * if (major) 0.86f else 0.93f
            drawLine(
                color = if (major) colors.onSurface else colors.onSurface.copy(alpha = 0.32f),
                start = c + d * inner, end = c + d * outer,
                strokeWidth = if (major) 2.2f * unit else 1f * unit,
                cap = StrokeCap.Round,
            )
        }
        val hourDeg = (time.hour % 12) * 30.0 + time.minute * 0.5
        val minuteDeg = time.minute * 6.0 + time.second * 0.1
        drawLine(colors.onSurface, c - dir(hourDeg) * (r * 0.08f), c + dir(hourDeg) * (r * 0.5f), 5.5f * unit, StrokeCap.Round)
        drawLine(colors.accent, c - dir(minuteDeg) * (r * 0.1f), c + dir(minuteDeg) * (r * 0.78f), 3f * unit, StrokeCap.Round)
        drawCircle(colors.accent, 4.5f * unit, c)
        drawCircle(colors.backgroundBottom, 1.8f * unit, c)
        drawCircle(colors.onSurface.copy(alpha = 0.12f), r, c, style = Stroke(1f * unit))
    }
}
