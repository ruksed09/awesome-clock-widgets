package com.versity.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val OrbitDate = DateTimeFormatter.ofPattern("EEEE, d MMMM")

/** Date, large clock and the battery / analogue / date-ring row. Shared by the Orbit home layout and the lock-style screen. */
@Composable
fun OrbitHeader(
    now: LocalDateTime,
    is24Hour: Boolean,
    settings: LauncherSettings,
    batteryPercent: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = LauncherTheme.colors
    val time = now.toLocalTime()
    val clockStyle = settings.clockStyle
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!settings.showsOwnDate) {
            Text(
                text = now.format(OrbitDate),
                color = colors.onSurface.copy(alpha = 0.85f),
                fontFamily = LauncherTheme.font,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
        CenteredClock(settings, now, is24Hour)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BatteryLevel(batteryPercent, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            if (clockStyle != ClockStyle.ANALOGUE) {
                AnalogueClockWidget(time, Modifier.size(64.dp), dense = true)
                Spacer(Modifier.width(12.dp))
            }
            DateRing(now.toLocalDate(), Modifier.size(76.dp))
        }
    }
}

/** The big clock in the chosen style, centred. Used by the Orbit home layout and the lock-style screen. */
@Composable
fun CenteredClock(settings: LauncherSettings, now: LocalDateTime, is24Hour: Boolean, modifier: Modifier = Modifier) {
    val time = now.toLocalTime()
    when (settings.clockStyle) {
        ClockStyle.STACKED -> StackedClock(time, is24Hour, modifier.fillMaxWidth(), sizeRange = 56f..92f, centered = true)
        ClockStyle.SINGLE_LINE -> SingleLineClock(
            time, is24Hour, modifier.fillMaxWidth(), sizeRange = 44f..104f,
            centered = true, widthFactor = 0.23f, weight = FontWeight.Bold, metallic = true,
        )
        ClockStyle.ANALOGUE -> BigAnalogueClock(time, modifier.size(150.dp))
        ClockStyle.ORNAMENT -> OrnamentClock(
            time, is24Hour, settings.ornament, modifier.fillMaxWidth(),
            date = now.toLocalDate().takeIf { settings.ornamentDate }, sizeRange = 56f..112f, widthFactor = 0.22f,
        )
    }
}

/** True when the clock draws the date itself, so screens skip their own date line. */
val LauncherSettings.showsOwnDate: Boolean
    get() = clockStyle == ClockStyle.ORNAMENT && ornamentDate
