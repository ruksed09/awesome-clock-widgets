package com.versity.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.HomeWidget
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.LauncherMenu
import com.versity.launcher.ui.components.LauncherMenuItem
import com.versity.launcher.ui.dimAlpha
import com.versity.launcher.ui.home.components.BigAnalogueClock
import com.versity.launcher.ui.home.components.BigClock
import com.versity.launcher.ui.home.components.BigClockStyles
import com.versity.launcher.ui.home.components.CentredClockBlock
import com.versity.launcher.ui.home.components.Dock
import com.versity.launcher.ui.home.components.FavouritesRow
import com.versity.launcher.ui.home.components.HomeWidgetsLayer
import com.versity.launcher.ui.home.components.OrnamentClock
import com.versity.launcher.ui.home.components.PlacedClock
import com.versity.launcher.ui.home.components.SwipeHint
import com.versity.launcher.ui.home.components.dockCapacity
import com.versity.launcher.ui.home.components.showsOwnDate
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.drawGlyph
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val settings: LauncherSettings = LauncherSettings.DEFAULT,
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

/** What the home screen can ask for. Defaults do nothing, so previews and tests can omit them. */
open class HomeActions {
    open fun openDrawer() {}
    open fun openCustomise() {}
    open fun openPhone() {}
    open fun setDefault() {}
    open fun dismissBanner() {}
    open fun launchApp(app: AppEntry) {}
    open fun removeFavourite(app: AppEntry) {}
    open fun editWidget(widget: HomeWidget) {}
    open fun arrange() {}
    open fun removeWidget(widget: HomeWidget) {}
}

/**
 * Gradient, wallpaper and dim overlay. Home, lock screen, drawer and every preview use this, so one
 * theme looks the same everywhere.
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
    }
}

@Composable
fun HomeContent(state: HomeUiState, modifier: Modifier = Modifier, actions: HomeActions = remember { HomeActions() }) = AppTheme(state.settings) {
    if (!state.loaded) {
        Box(modifier.fillMaxSize().background(Color.Black))
        return@AppTheme
    }
    val current by rememberUpdatedState(actions)
    val swipe = with(LocalDensity.current) { 72.dp.toPx() }
    var widgetMenu by remember { mutableStateOf<HomeWidget?>(null) }

    Box(modifier.fillMaxSize()) {
        HomeBackground(state.settings)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                // Long-press on empty space opens Customise; a swipe up opens all apps.
                .pointerInput(Unit) { detectTapGestures(onLongPress = { current.openCustomise() }) }
                .pointerInput(Unit) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dy = 0f },
                        onDragEnd = { if (dy < -swipe) current.openDrawer() },
                    ) { _, amount -> dy += amount }
                }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Customise") { current.openCustomise(); true },
                        CustomAccessibilityAction("All apps") { current.openDrawer(); true },
                    )
                }
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // Side-by-side only in landscape; a tablet held upright keeps one centred column.
            val wide = maxWidth >= WideBreakpoint && maxWidth > maxHeight
            HomeWidgetsLayer(state.settings.homeWidgets, minute = state.now) { widgetMenu = it }
            when (state.settings.layout) {
                HomeLayout.MINIMAL -> MinimalLayout(state, current, wide)
                HomeLayout.ORBIT -> CentredLayout(state, current, wide)
            }
            state.settings.clockPlacement?.let { PlacedClock(it, state.settings, state.now, state.is24Hour) }
            CustomiseButton({ current.openCustomise() }, Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 10.dp))
            widgetMenu?.let { w ->
                Box(Modifier.align(Alignment.Center)) {
                    LauncherMenu(expanded = true, onDismiss = { widgetMenu = null }) {
                        LauncherMenuItem("Edit widget") { widgetMenu = null; current.editWidget(w) }
                        LauncherMenuItem("Move and resize") { widgetMenu = null; current.arrange() }
                        LauncherMenuItem("Remove from home") { widgetMenu = null; current.removeWidget(w) }
                    }
                }
            }
        }
    }
}

/** Favourites beyond the dock's capacity, the banner, the swipe hint and the dock itself. */
@Composable
private fun BottomArea(state: HomeUiState, actions: HomeActions) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        val capacity = dockCapacity(minOf(maxWidth, 520.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val overflow = state.favourites.drop(capacity)
            if (overflow.isNotEmpty()) {
                FavouritesRow(overflow, actions::launchApp, actions::removeFavourite)
                Spacer(Modifier.height(16.dp))
            }
            if (state.showDefaultBanner) {
                DefaultBanner(actions::setDefault, actions::dismissBanner)
                Spacer(Modifier.height(14.dp))
            }
            SwipeHint()
            Spacer(Modifier.height(10.dp))
            Dock(
                favourites = state.favourites, capacity = capacity,
                onLaunch = actions::launchApp, onRemove = actions::removeFavourite,
                onPhone = actions::openPhone, onApps = actions::openDrawer,
            )
        }
    }
}

@Composable
private fun MinimalLayout(state: HomeUiState, actions: HomeActions, wide: Boolean) {
    if (wide) {
        Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) { ClockBlock(state) }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) { BottomArea(state, actions) }
        }
    } else {
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 640.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            ClockBlock(state)
            Spacer(Modifier.weight(1f))
            BottomArea(state, actions)
        }
    }
}

@Composable
private fun CentredLayout(state: HomeUiState, actions: HomeActions, wide: Boolean) {
    val s = state.settings
    if (wide) {
        Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                CentredClockBlock(state.now, state.is24Hour, s)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) { BottomArea(state, actions) }
        }
    } else {
        Column(
            Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 640.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))
            CentredClockBlock(state.now, state.is24Hour, s)
            Spacer(Modifier.weight(1f))
            BottomArea(state, actions)
        }
    }
}

@Composable
internal fun ClockBlock(state: HomeUiState) {
    // A freely placed clock (and its date) is drawn over the layout instead.
    if (state.settings.clockPlacement != null) return
    val time = state.now.toLocalTime()
    val style = state.settings.clockStyle
    val centered = style == ClockStyle.ORNAMENT || style in BigClockStyles
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        when (style) {
            ClockStyle.ANALOGUE -> BigAnalogueClock(time, Modifier.padding(top = 24.dp).size(176.dp))
            ClockStyle.ORNAMENT -> OrnamentClock(
                time, state.is24Hour, state.settings.ornament, Modifier.fillMaxWidth(),
                date = state.now.toLocalDate().takeIf { state.settings.ornamentDate },
                sizeRange = 48f..96f, centered = true,
            )
            else -> BigClock(style, time, state.is24Hour, Modifier.fillMaxWidth().padding(top = 36.dp), centered = true, date = state.now.toLocalDate())
        }
        if (!state.settings.showsOwnDate) {
            Text(
                text = state.now.format(HomeDateFormat),
                color = LauncherTheme.colors.onSurface.copy(alpha = 0.85f),
                fontFamily = LauncherTheme.font,
                fontSize = 15.sp,
            )
        }
    }
}

/** One gentle, dismissible card. Tapping "Set" opens the system chooser once; it never re-prompts. */
@Composable
private fun DefaultBanner(onSet: () -> Unit, onDismiss: () -> Unit) {
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .testTag(TestTags.DEFAULT_BANNER)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.onSurface.copy(alpha = 0.08f))
            .border(1.dp, colors.onSurface.copy(alpha = 0.12f), shape)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Make Versity your home app",
            color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.testTag(TestTags.DEFAULT_BANNER_SET).heightIn(min = 48.dp).clickable(onClick = onSet)
                .padding(horizontal = 12.dp).semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { Text("Set", color = colors.accent, fontFamily = LauncherTheme.font, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        Box(
            Modifier.testTag(TestTags.DEFAULT_BANNER_DISMISS).size(48.dp).clickable(onClick = onDismiss)
                .semantics { contentDescription = "Dismiss"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { Canvas(Modifier.size(16.dp)) { drawGlyph(Glyph.CLOSE, colors.muted) } }
    }
}

/** Small "tune" glyph: the visible way into Customise, alongside long-press. */
@Composable
private fun CustomiseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    Box(
        modifier
            .testTag(TestTags.OPEN_CUSTOMISE)
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Customise"; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Canvas(Modifier.size(20.dp)) { drawGlyph(Glyph.SETTINGS, colors.onSurface.copy(alpha = 0.9f)) } }
}
