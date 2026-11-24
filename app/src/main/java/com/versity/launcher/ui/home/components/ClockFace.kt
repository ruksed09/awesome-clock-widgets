package com.versity.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.home.HomeDateFormat
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The whole clock unit for a freely placed clock: the face in the chosen style plus its date, sized
 * by the width it is given (so the placement's scale really scales it, without the layouts' caps).
 */
@Composable
fun ClockFace(settings: LauncherSettings, now: LocalDateTime, is24Hour: Boolean, modifier: Modifier = Modifier) {
    val time = now.toLocalTime()
    val showDate = !settings.showsOwnDate
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (settings.clockStyle) {
            ClockStyle.STACKED -> StackedClock(time, is24Hour, Modifier.fillMaxWidth(), sizeRange = 24f..260f, centered = true)
            ClockStyle.SINGLE_LINE -> SingleLineClock(time, is24Hour, Modifier.fillMaxWidth(), sizeRange = 20f..220f, centered = true, widthFactor = 0.26f)
            ClockStyle.ANALOGUE -> BigAnalogueClock(time, Modifier.fillMaxWidth(0.62f).let { it })
            ClockStyle.ORNAMENT -> OrnamentClock(
                time, is24Hour, settings.ornament, Modifier.fillMaxWidth(),
                date = now.toLocalDate().takeIf { settings.ornamentDate }, sizeRange = 24f..220f, widthFactor = 0.24f,
            )
            else -> BigClock(settings.clockStyle, time, is24Hour, Modifier.fillMaxWidth(), maxSize = 2000.dp, date = now.toLocalDate())
        }
        if (showDate) {
            Text(now.format(HomeDateFormat), color = LauncherTheme.colors.onSurface.copy(alpha = 0.85f), fontFamily = LauncherTheme.font, fontSize = 15.sp, maxLines = 1)
        }
    }
}

/**
 * Lays [ClockFace] out at the user's placement: centred on (x, y) of the area, [ClockPlacement.scale]
 * times the default width, and kept fully on screen.
 */
@Composable
fun PlacedClock(placement: ClockPlacement, settings: LauncherSettings, now: LocalDateTime, is24Hour: Boolean, modifier: Modifier = Modifier) {
    Layout(
        content = { ClockFace(settings, now, is24Hour) },
        modifier = modifier.fillMaxSize().testTag(TestTags.PLACED_CLOCK),
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val target = (w * 0.86f * placement.scale).roundToInt().coerceIn(1, w)
        // Height is capped too, so even a large clock in landscape stays fully on screen.
        val child = measurables.first().measure(Constraints(minWidth = target, maxWidth = target, maxHeight = (h * 0.92f).roundToInt()))
        val x = (placement.x * w - child.width / 2f).roundToInt().coerceIn(0, max(0, w - child.width))
        val y = (placement.y * h - child.height / 2f).roundToInt().coerceIn(0, max(0, h - child.height))
        layout(w, h) { child.place(x, y) }
    }
}
