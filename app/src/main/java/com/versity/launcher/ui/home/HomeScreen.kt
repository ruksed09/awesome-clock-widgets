package com.versity.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.ui.home.components.FavouritesRow
import com.versity.launcher.ui.backdrop.BackdropLayer
import com.versity.launcher.ui.home.components.OrbitHeader
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.dimAlpha
import com.versity.launcher.ui.home.components.AnalogueClockWidget
import com.versity.launcher.ui.home.components.BatteryBar
import com.versity.launcher.ui.home.components.BigAnalogueClock
import com.versity.launcher.ui.home.components.DateRing
import com.versity.launcher.ui.home.components.Dock
import com.versity.launcher.ui.home.components.NotificationPill
import com.versity.launcher.ui.home.components.SingleLineClock
import com.versity.launcher.ui.home.components.StackedClock
import com.versity.launcher.ui.home.components.OrnamentClock
import com.versity.launcher.ui.home.components.showsOwnDate
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val settings: LauncherSettings = LauncherSettings.DEFAULT,
    val batteryPercent: Int? = null,
    val now: LocalDateTime = LocalDateTime.of(2026, 11, 18, 4, 22),
    val isDefaultLauncher: Boolean = true,
    val is24Hour: Boolean = true,
    val defaultBannerDismissed: Boolean = false,
    val favourites: List<AppEntry> = emptyList(),
    /** False until DataStore has delivered the saved settings; nothing themed is drawn before that. */
    val loaded: Boolean = true,
) {
    val showDefaultBanner: Boolean get() = !isDefaultLauncher && !defaultBannerDismissed
}

private val WideBreakpoint = 600.dp
internal val HomeDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

private class HomeActions(
    val onOpenDrawer: () -> Unit,
    val onOpenPhone: () -> Unit,
    val onSetDefault: () -> Unit,
    val onDismissBanner: () -> Unit,
    val onLaunchApp: (AppEntry) -> Unit,
    val onRemoveFavourite: (AppEntry) -> Unit,
)

/**
 * Gradient, wallpaper, dim overlay and the theme's backdrop. Home, lock-style screen, drawer and the
 * Customise preview all use this, so one theme looks the same everywhere.
 */
@Composable
fun HomeBackground(settings: LauncherSettings, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    val dim = settings.dimPercent.coerceIn(LauncherSettings.MIN_DIM, LauncherSettings.MAX_DIM) / 100f
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))) {
        WallpaperLayer(settings.wallpaperUri, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .testTag(TestTags.DIM_OVERLAY)
                .semantics { dimAlpha = dim }
                .background(Color.Black.copy(alpha = dim)),
        )
        BackdropLayer(settings.backdrop, ThemePresets.palette(settings.paletteId), colors.accent, Modifier.fillMaxSize())
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {},
    onOpenCustomise: () -> Unit = {},
    onSetDefault: () -> Unit = {},
    onOpenPhone: () -> Unit = {},
    onDismissBanner: () -> Unit = {},
    onLaunchApp: (AppEntry) -> Unit = {},
    onRemoveFavourite: (AppEntry) -> Unit = {},
) = AppTheme(state.settings) {
    if (!state.loaded) {
        Box(modifier.fillMaxSize().background(Color.Black))
        return@AppTheme
    }
    val currentOpenCustomise by rememberUpdatedState(onOpenCustomise)
    val layout = state.settings.layout
    val actions = HomeActions(onOpenDrawer, onOpenPhone, onSetDefault, onDismissBanner, onLaunchApp, onRemoveFavourite)

    Box(modifier.fillMaxSize()) {
        HomeBackground(state.settings)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                // Long-press on empty space opens Customise. Children that consume the press (favourites) win.
                .pointerInput(Unit) { detectTapGestures(onLongPress = { currentOpenCustomise() }) }
                .semantics {
                    customActions = listOf(CustomAccessibilityAction("Customise") { onOpenCustomise(); true })
                }
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            val wide = maxWidth >= WideBreakpoint
            when (layout) {
                HomeLayout.CLASSIC -> ClassicLayout(state, actions, wide)
                HomeLayout.MINIMAL -> MinimalLayout(state, actions, wide)
                HomeLayout.ORBIT -> OrbitLayout(state, actions, wide)
            }
            CustomiseButton(onOpenCustomise, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp))
        }
    }
}

@Composable
private fun Favourites(state: HomeUiState, actions: HomeActions) =
    FavouritesRow(state.favourites, actions.onLaunchApp, actions.onRemoveFavourite)

@Composable
private fun BottomArea(state: HomeUiState, actions: HomeActions) {
    if (state.showDefaultBanner) {
        DefaultBanner(actions.onSetDefault, actions.onDismissBanner)
        Spacer(Modifier.height(16.dp))
    }
    Dock(onPhone = actions.onOpenPhone, onApps = actions.onOpenDrawer, Modifier.fillMaxWidth())
}

@Composable
private fun ClassicLayout(state: HomeUiState, actions: HomeActions, wide: Boolean) {
    if (wide) {
        Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f)) {
                ClockBlock(state)
                Spacer(Modifier.height(16.dp))
                NotificationPill()
                Spacer(Modifier.height(16.dp))
                Favourites(state, actions)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                WidgetsBlock(state)
                Spacer(Modifier.height(24.dp))
                BottomArea(state, actions)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            ClockBlock(state)
            Spacer(Modifier.height(16.dp))
            NotificationPill()
            Spacer(Modifier.height(16.dp))
            Favourites(state, actions)
            Spacer(Modifier.weight(1f))
            WidgetsBlock(state)
            Spacer(Modifier.height(24.dp))
            BottomArea(state, actions)
        }
    }
}

@Composable
private fun MinimalLayout(state: HomeUiState, actions: HomeActions, wide: Boolean) {
    if (wide) {
        Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f)) { ClockBlock(state) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                Favourites(state, actions)
                Spacer(Modifier.height(24.dp))
                BottomArea(state, actions)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            ClockBlock(state)
            Spacer(Modifier.weight(1f))
            Favourites(state, actions)
            Spacer(Modifier.height(24.dp))
            BottomArea(state, actions)
        }
    }
}

@Composable
private fun OrbitLayout(state: HomeUiState, actions: HomeActions, wide: Boolean) {
    val s = state.settings
    if (wide) {
        Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                OrbitHeader(state.now, state.is24Hour, s, state.batteryPercent)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                NotificationPill()
                Spacer(Modifier.height(16.dp))
                Favourites(state, actions)
                Spacer(Modifier.height(24.dp))
                BottomArea(state, actions)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            OrbitHeader(state.now, state.is24Hour, s, state.batteryPercent)
            Spacer(Modifier.weight(1f))
            NotificationPill()
            Spacer(Modifier.height(16.dp))
            Favourites(state, actions)
            Spacer(Modifier.height(24.dp))
            BottomArea(state, actions)
        }
    }
}

@Composable
internal fun ClockBlock(state: HomeUiState) {
    val time = state.now.toLocalTime()
    val centered = state.settings.clockStyle == ClockStyle.ORNAMENT && state.settings.layout == HomeLayout.MINIMAL
    Column(
        if (centered) Modifier.fillMaxWidth() else Modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        when (state.settings.clockStyle) {
            ClockStyle.STACKED -> StackedClock(time, state.is24Hour, Modifier.fillMaxWidth())
            ClockStyle.SINGLE_LINE -> SingleLineClock(time, state.is24Hour, Modifier.fillMaxWidth())
            ClockStyle.ANALOGUE -> BigAnalogueClock(time, Modifier.size(160.dp))
            ClockStyle.ORNAMENT -> OrnamentClock(
                time, state.is24Hour, state.settings.ornament, Modifier.fillMaxWidth(),
                date = state.now.toLocalDate().takeIf { state.settings.ornamentDate },
                sizeRange = 48f..96f, centered = centered,
            )
        }
        if (!state.settings.showsOwnDate) {
            Text(
                text = state.now.format(HomeDateFormat),
                color = LauncherTheme.colors.muted,
                fontFamily = LauncherTheme.font,
                fontSize = 14.sp,
            )
        }
    }
}

/** Battery bar, then the analogue widget and date ring. The widget is hidden when the big clock is already analogue. */
@Composable
private fun WidgetsBlock(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BatteryBar(state.batteryPercent, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.settings.clockStyle != ClockStyle.ANALOGUE) {
                AnalogueClockWidget(state.now.toLocalTime(), Modifier.size(96.dp))
            }
            DateRing(state.now.toLocalDate(), Modifier.size(96.dp))
        }
    }
}

/** One gentle, dismissible row. Tapping "Set" opens the system chooser once; it never re-prompts. */
@Composable
private fun DefaultBanner(onSet: () -> Unit, onDismiss: () -> Unit) {
    val colors = LauncherTheme.colors
    Row(
        Modifier
            .testTag(TestTags.DEFAULT_BANNER)
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(2.dp))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Make Versity your home app",
            color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.testTag(TestTags.DEFAULT_BANNER_SET).heightIn(min = 48.dp).clickable(onClick = onSet)
                .padding(horizontal = 12.dp).semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { Text("Set", color = colors.accent, fontFamily = LauncherTheme.font, fontSize = 12.sp) }
        Box(
            Modifier.testTag(TestTags.DEFAULT_BANNER_DISMISS).size(48.dp).clickable(onClick = onDismiss)
                .semantics { contentDescription = "Dismiss"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(12.dp)) {
                val sw = 1.5.dp.toPx()
                drawLine(colors.muted, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = sw)
                drawLine(colors.muted, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = sw)
            }
        }
    }
}

/** Small "tune" glyph: the visible way into Customise, alongside long-press. */
@Composable
private fun CustomiseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    Box(
        modifier
            .testTag(TestTags.OPEN_CUSTOMISE)
            .size(40.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Customise"; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val w = size.width
            val stroke = 1.5.dp.toPx()
            listOf(0.2f to 0.35f, 0.5f to 0.7f, 0.8f to 0.45f).forEach { (y, knob) ->
                drawLine(colors.onSurface, Offset(0f, w * y), Offset(w, w * y), strokeWidth = stroke)
                drawCircle(colors.backgroundBottom, 2.5.dp.toPx(), Offset(w * knob, w * y))
                drawCircle(colors.accent, 2.5.dp.toPx(), Offset(w * knob, w * y), style = Stroke(stroke))
            }
        }
    }
}
