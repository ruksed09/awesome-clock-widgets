package com.versity.launcher.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CentredDate = DateTimeFormatter.ofPattern("EEEE, d MMMM")

/**
 * Date over the large clock, centred. Shared by the Centered home layout and the lock screen. A freely
 * placed clock (and its date) is drawn by the screen instead, so this draws nothing then.
 */
@Composable
fun CentredClockBlock(now: LocalDateTime, is24Hour: Boolean, settings: LauncherSettings, modifier: Modifier = Modifier) {
    if (settings.clockPlacement != null) return
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!settings.showsOwnDate) {
            Text(
                text = now.format(CentredDate),
                color = LauncherTheme.colors.onSurface.copy(alpha = 0.85f),
                fontFamily = LauncherTheme.font,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        CenteredClock(settings, now, is24Hour)
    }
}

/** The big clock in the chosen style, centred. */
@Composable
fun CenteredClock(settings: LauncherSettings, now: LocalDateTime, is24Hour: Boolean, modifier: Modifier = Modifier) {
    val time = now.toLocalTime()
    when (settings.clockStyle) {
        ClockStyle.ANALOGUE -> BigAnalogueClock(time, modifier.fillMaxWidth(0.56f))
        ClockStyle.ORNAMENT -> OrnamentClock(
            time, is24Hour, settings.ornament, modifier.fillMaxWidth(),
            date = now.toLocalDate().takeIf { settings.ornamentDate }, sizeRange = 56f..112f, widthFactor = 0.22f,
        )
        else -> BigClock(settings.clockStyle, time, is24Hour, modifier.fillMaxWidth(), date = now.toLocalDate())
    }
}

/** True when the clock draws the date itself, so screens skip their own date line. */
val LauncherSettings.showsOwnDate: Boolean
    get() = (clockStyle == ClockStyle.ORNAMENT && ornamentDate) || clockStyle == ClockStyle.STAGE
