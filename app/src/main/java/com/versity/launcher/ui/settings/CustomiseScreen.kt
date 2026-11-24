package com.versity.launcher.ui.settings

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.IconStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.Ornament
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.domain.ThemeCategory
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.lockLook
import com.versity.launcher.domain.matches
import com.versity.launcher.domain.withTheme
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.backdrop.BackdropLayer
import com.versity.launcher.ui.components.HomePreview
import com.versity.launcher.ui.components.LockPreview
import com.versity.launcher.ui.home.components.OrnamentClock
import com.versity.launcher.ui.previewPalette
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.fontFamilyFor
import com.versity.launcher.widget.ClockWidgetKind
import java.time.LocalDate
import java.time.LocalTime

interface CustomiseActions {
    fun onPickWallpaper() {}
    fun onClearWallpaper() {}
    fun onDim(percent: Int) {}
    fun onClockStyle(style: ClockStyle) {}
    fun onFont(id: String) {}
    fun onPalette(id: String) {}
    fun onAccent(argb: Long?) {}
    fun onLayout(layout: HomeLayout) {}
    fun onLockStyle(enabled: Boolean) {}
    fun onBackdrop(backdrop: Backdrop) {}
    fun onOrnament(ornament: Ornament) {}
    fun onOrnamentDate(show: Boolean) {}
    fun onDrawerStyle(style: DrawerStyle) {}
    fun onIconStyle(style: IconStyle) {}
    fun onApplyTheme(request: ThemeApplyRequest) {}
    fun onLockFollowsHome() {}
    fun onAddWidget(kind: ClockWidgetKind) {}
    fun onPreviewLock() {}
    fun onSetLockWallpaper() {}
    fun onReset() {}
}

enum class CustomiseTab(val label: String) { THEMES("Themes"), STYLE("Style"), LOCK("Lock screen"), WIDGETS("Widgets") }

enum class ApplyTarget(val label: String) {
    HOME("Home"), LOCK("Lock"), BOTH("Both");

    val home: Boolean get() = this != LOCK
    val lock: Boolean get() = this != HOME
}

/** Everything the apply sheet decided. Options that do not fit the target are false. */
data class ThemeApplyRequest(
    val theme: ThemeBundle,
    val target: ApplyTarget,
    /** Home: drop the photo wallpaper so the theme's own background shows. */
    val useThemeBackground: Boolean,
    /** Home: also render the theme to the phone's home wallpaper (shown behind other apps). */
    val setPhoneWallpaper: Boolean,
    /** Lock: show the themed lock screen over the system lock screen while locked. */
    val showLockScreen: Boolean,
    /** Lock: also render the theme to the phone's lock-screen wallpaper. */
    val setLockWallpaper: Boolean,
)

private val Corner = RoundedCornerShape(2.dp)
private val PreviewTime: LocalTime = LocalTime.of(11, 11)
private val PreviewDate: LocalDate = LocalDate.of(2026, 11, 11)
private val AccentSwatches = listOf(
    0xFF8FA3FFL, 0xFF4FC3E8L, 0xFF5ED6A0L, 0xFFFFD166L,
    0xFFFF8A5CL, 0xFFFF6B8BL, 0xFFC792EAL, 0xFFE8EAF2L,
)

private fun backdropLabel(b: Backdrop) = when (b) {
    Backdrop.NONE -> "None"
    Backdrop.ORBIT -> "Orbit"
    Backdrop.RINGS -> "Rings"
    Backdrop.HORIZON -> "Horizon"
    Backdrop.WAVES -> "Waves"
    Backdrop.STARS -> "Stars"
    Backdrop.AURORA -> "Aurora"
    Backdrop.DUNES -> "Dunes"
    Backdrop.PEAKS -> "Peaks"
    Backdrop.ECLIPSE -> "Eclipse"
    Backdrop.BOKEH -> "Bokeh"
    Backdrop.GRID -> "Grid"
    Backdrop.ARCS -> "Arcs"
    Backdrop.RAIN -> "Rain"
}

private fun layoutLabel(l: HomeLayout) = when (l) {
    HomeLayout.CLASSIC -> "Classic"
    HomeLayout.MINIMAL -> "Minimal"
    HomeLayout.ORBIT -> "Orbit"
}

private fun clockStyleLabel(s: ClockStyle) = when (s) {
    ClockStyle.STACKED -> "Stacked"
    ClockStyle.SINGLE_LINE -> "Single line"
    ClockStyle.ANALOGUE -> "Analogue"
    ClockStyle.ORNAMENT -> "Ornament"
}

private fun ornamentLabel(o: Ornament) = when (o) {
    Ornament.CELESTIAL -> "Celestial line"
    Ornament.UNDERLINE -> "Underline"
    Ornament.MOON -> "Moon divider"
    Ornament.CORNERS -> "Frame corners"
}

/** The theme the settings currently match, or null for a custom mix. */
private fun currentHomeTheme(s: LauncherSettings): ThemeBundle? = ThemePresets.themes.firstOrNull { it.matches(s) }

private fun lockThemeLabel(s: LauncherSettings): String =
    s.lockThemeId?.let(ThemePresets::theme)?.name ?: "Same as home screen"

@Composable
fun CustomiseContent(
    settings: LauncherSettings,
    actions: CustomiseActions,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    lockWallpaperStatus: String? = null,
    widgetStatus: String? = null,
    initialTab: CustomiseTab = CustomiseTab.THEMES,
) = AppTheme(settings) {
    val colors = LauncherTheme.colors
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var sheetThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    val sheetTheme = sheetThemeId?.let(ThemePresets::theme)

    Box(modifier.fillMaxSize().background(colors.backgroundBottom)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            TopBar(onBack, actions::onReset)
            Tabs(tab) { tab = it }
            Crossfade(tab, animationSpec = tween(150), label = "tab") { current ->
                when (current) {
                    CustomiseTab.THEMES -> ThemesTab(settings) { sheetThemeId = it.id }
                    CustomiseTab.STYLE -> StyleTab(settings, actions)
                    CustomiseTab.LOCK -> LockTab(
                        settings, actions, lockWallpaperStatus,
                        onBrowseThemes = { tab = CustomiseTab.THEMES },
                        onRequestLockWallpaper = { showLockDialog = true },
                    )
                    CustomiseTab.WIDGETS -> WidgetsTab(actions, widgetStatus)
                }
            }
        }
        AnimatedVisibility(visible = sheetTheme != null, enter = fadeIn(tween(150)), exit = fadeOut(tween(120))) {
            // Keep the last theme on screen while the sheet fades out.
            var shown by remember { mutableStateOf(sheetTheme) }
            if (sheetTheme != null) shown = sheetTheme
            shown?.let { theme ->
                ThemeApplySheet(
                    theme = theme,
                    settings = settings,
                    onDismiss = { sheetThemeId = null },
                    onApply = { request -> sheetThemeId = null; actions.onApplyTheme(request) },
                )
            }
        }
    }
    if (showLockDialog) {
        LockWallpaperDialog(
            onConfirm = { showLockDialog = false; actions.onSetLockWallpaper() },
            onCancel = { showLockDialog = false },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onReset: () -> Unit) {
    val colors = LauncherTheme.colors
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clickable(onClick = onBack)
                .semantics { contentDescription = "Back"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val w = size.width
                val sw = 1.5.dp.toPx()
                drawLine(colors.onSurface, Offset(w * 0.65f, w * 0.15f), Offset(w * 0.3f, w * 0.5f), strokeWidth = sw)
                drawLine(colors.onSurface, Offset(w * 0.3f, w * 0.5f), Offset(w * 0.65f, w * 0.85f), strokeWidth = sw)
            }
        }
        Text("Customise", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(
            Modifier.testTag(TestTags.RESET).heightIn(min = 48.dp).clickable(onClick = onReset)
                .padding(horizontal = 12.dp).semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Text("Reset", color = colors.accent, fontFamily = LauncherTheme.font, fontSize = 13.sp)
        }
    }
}

/** Text tabs with a short underline, scrollable when the screen is narrow. */
@Composable
private fun Tabs(selected: CustomiseTab, onSelect: (CustomiseTab) -> Unit) {
    val colors = LauncherTheme.colors
    Column {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            CustomiseTab.entries.forEach { t ->
                val on = t == selected
                Column(
                    Modifier
                        .testTag(TestTags.tab(t.name))
                        .heightIn(min = 44.dp)
                        .clickable { onSelect(t) }
                        .padding(horizontal = 12.dp)
                        .semantics { this.selected = on; role = Role.Tab },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        t.label,
                        color = if (on) colors.onSurface else colors.muted,
                        fontFamily = LauncherTheme.font,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
                    )
                    Box(Modifier.width(24.dp).height(2.dp).background(if (on) colors.accent else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

// ---------------------------------------------------------------- Themes

@Composable
private fun ThemesTab(settings: LauncherSettings, onOpen: (ThemeBundle) -> Unit) {
    var category by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    val list = remember(category) { ThemePresets.themes.filter { category == null || it.category == category } }
    val home = currentHomeTheme(settings)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatusPair("Home", home?.name ?: "Custom")
                    StatusPair("Lock", lockThemeLabel(settings))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("All ${ThemePresets.themes.size}", category == null, TestTags.category("ALL")) { category = null }
                    ThemeCategory.entries.forEach { c ->
                        Choice(c.label, category == c, TestTags.category(c.name)) { category = c }
                    }
                }
                Hint("Tap a theme to preview it and choose where it goes: home screen, lock screen or both.")
            }
        }
        items(list, key = { it.id }) { theme ->
            ThemeCard(
                theme = theme,
                onHome = home?.id == theme.id,
                onLock = settings.lockThemeId == theme.id || (settings.lockThemeId == null && home?.id == theme.id),
                onClick = { onOpen(theme) },
            )
        }
    }
}

@Composable
private fun StatusPair(label: String, value: String) {
    val colors = LauncherTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 10.sp, letterSpacing = 0.8.sp)
        Text(value, color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 12.sp, maxLines = 1)
    }
}

/** Gallery card: the real home screen in this theme, scaled down, then its name and tagline. */
@Composable
private fun ThemeCard(theme: ThemeBundle, onHome: Boolean, onLock: Boolean, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    val look = remember(theme.id) { LauncherSettings.DEFAULT.withTheme(theme) }
    val where = listOfNotNull("home screen".takeIf { onHome }, "lock screen".takeIf { onLock })
    Column(
        Modifier
            .testTag(TestTags.theme(theme.id))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = onHome || onLock
                if (where.isNotEmpty()) stateDescription = "On ${where.joinToString(" and ")}"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            HomePreview(look, maxWidth, border = if (onHome || onLock) colors.accent else colors.border, onClick = onClick)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                theme.name, color = if (onHome || onLock) colors.accent else colors.onSurface, fontFamily = LauncherTheme.font,
                fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f, fill = false),
            )
            if (onHome) Badge("HOME")
            if (onLock) Badge("LOCK")
        }
        Text(theme.tagline, color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2)
    }
}

@Composable
private fun Badge(text: String) {
    val colors = LauncherTheme.colors
    Text(
        text,
        color = colors.accent,
        fontFamily = LauncherTheme.font,
        fontSize = 9.sp,
        letterSpacing = 0.6.sp,
        maxLines = 1,
        modifier = Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), Corner).padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

/** Bottom panel: large home and lock previews, where to apply, and what else to change on the phone. */
@Composable
private fun ThemeApplySheet(
    theme: ThemeBundle,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (ThemeApplyRequest) -> Unit,
) {
    val colors = LauncherTheme.colors
    var target by rememberSaveable(theme.id) { mutableStateOf(ApplyTarget.BOTH) }
    var useThemeBackground by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setPhoneWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var showLockScreen by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setLockWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    BackHandler(onBack = onDismiss)

    val hasPhoto = settings.wallpaperUri != null
    val themedHome = settings.withTheme(theme).copy(wallpaperUri = if (useThemeBackground) null else settings.wallpaperUri)
    val homeAfter = if (target.home) themedHome else settings
    val lockAfter = when (target) {
        // Home only: a lock screen that follows home changes too; one with its own theme does not.
        ApplyTarget.HOME -> if (settings.lockThemeId == null) themedHome else settings.lockLook()
        ApplyTarget.LOCK -> settings.withTheme(theme).copy(wallpaperUri = null)
        ApplyTarget.BOTH -> themedHome
    }.copy(lockThemeId = null)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
        Column(
            Modifier
                .testTag(TestTags.APPLY_SHEET)
                .align(Alignment.BottomCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.border)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(theme.category.label.uppercase(), color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 10.sp, letterSpacing = 0.8.sp)
                    Text(theme.name, color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(theme.tagline, color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)
                }
                CloseButton(onDismiss)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val each: Dp = min((maxWidth - 12.dp) / 2, 132.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    LabelledPreview("Home screen", changed = target.home) { HomePreview(homeAfter, each, border = colors.border) }
                    LabelledPreview("Lock screen", changed = target.lock || (target.home && settings.lockThemeId == null)) {
                        LockPreview(lockAfter, each, border = colors.border)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label("Apply to")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApplyTarget.entries.forEach { t ->
                        Choice(t.label, target == t, TestTags.applyTarget(t.name), modifier = Modifier.weight(1f)) { target = t }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (target.home && hasPhoto) {
                    CheckRow("Use the theme's background instead of my photo", null, useThemeBackground, TestTags.OPT_THEME_BACKGROUND) { useThemeBackground = it }
                }
                if (target.home) {
                    CheckRow("Also set as phone wallpaper", "Shown behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
                }
                if (target.lock) {
                    CheckRow(
                        "Show this lock screen while the phone is locked",
                        "Appears over the system lock screen. Unlocking still needs your PIN, fingerprint or face.",
                        showLockScreen, TestTags.OPT_LOCK_SCREEN,
                    ) { showLockScreen = it }
                    CheckRow(
                        "Also set as phone lock-screen wallpaper",
                        "Replaces your current lock-screen wallpaper. The phone's own clock stays on top.",
                        setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER,
                    ) { setLockWallpaper = it }
                }
            }
        }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton("Cancel", colors.muted, Modifier.testTag(TestTags.APPLY_CANCEL), onDismiss)
                PrimaryButton("Apply", Modifier.testTag(TestTags.APPLY_CONFIRM)) {
                    onApply(
                        ThemeApplyRequest(
                            theme = theme,
                            target = target,
                            useThemeBackground = target.home && hasPhoto && useThemeBackground,
                            setPhoneWallpaper = target.home && setPhoneWallpaper,
                            showLockScreen = target.lock && showLockScreen,
                            setLockWallpaper = target.lock && setLockWallpaper,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelledPreview(label: String, changed: Boolean, content: @Composable () -> Unit) {
    val colors = LauncherTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Text(
            if (changed) label else "$label · unchanged",
            color = if (changed) colors.onSurface else colors.muted,
            fontFamily = LauncherTheme.font,
            fontSize = 11.sp,
        )
    }
}

// ---------------------------------------------------------------- Style

@Composable
private fun StyleTab(settings: LauncherSettings, actions: CustomiseActions) {
    // Sliders hold the finger position locally and save on release, so dragging never waits on disk.
    // The preview reads the live value, so it still follows the finger.
    var dragDim by remember { mutableStateOf<Int?>(null) }
    var dragAccent by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(settings.dimPercent) { dragDim = null }
    LaunchedEffect(settings.accentArgb) { dragAccent = null }
    val live = settings.copy(dimPercent = dragDim ?: settings.dimPercent, accentArgb = dragAccent ?: settings.accentArgb)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val controls: @Composable () -> Unit = { StyleControls(settings, actions, live, { dragDim = it }, { dragAccent = it }) }
        when {
            w >= 720.dp -> Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
                    Previews(live, min((w / 2 - 56.dp) / 2, (h - 64.dp) * (360f / 780f)))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) { controls() }
            }
            // Tall phones keep the previews pinned while the controls scroll underneath.
            h >= 600.dp -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Previews(live, min((w - 48.dp) / 2, h * 0.3f * (360f / 780f)))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(LauncherTheme.colors.border))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) { controls() }
            }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Previews(live, min((w - 48.dp) / 2, 120.dp))
                }
                controls()
            }
        }
    }
}

/** Home and lock side by side, [each] wide. Tagged so tests can read which palette is previewed. */
@Composable
private fun Previews(settings: LauncherSettings, each: Dp) {
    val colors = LauncherTheme.colors
    Row(
        Modifier.testTag(TestTags.PREVIEW).semantics { previewPalette = settings.paletteId },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LabelledPreview("Home screen", changed = true) { HomePreview(settings, each, border = colors.border) }
        LabelledPreview("Lock screen", changed = true) { LockPreview(settings, each, border = colors.border) }
    }
}

@Composable
private fun StyleControls(
    settings: LauncherSettings,
    actions: CustomiseActions,
    live: LauncherSettings,
    onDimDrag: (Int?) -> Unit,
    onAccentDrag: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Section("Wallpaper") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Choice("Choose from device", false, TestTags.PICK_WALLPAPER) { actions.onPickWallpaper() }
                if (settings.wallpaperUri != null) {
                    Choice("Clear", false, TestTags.CLEAR_WALLPAPER) { actions.onClearWallpaper() }
                }
            }
            Hint(if (settings.wallpaperUri == null) "Using the theme background" else "Custom photo")
        }
        Section("Dim") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThinSlider(
                    value = live.dimPercent.toFloat(),
                    onChange = { onDimDrag(it.toInt()) },
                    onFinished = { actions.onDim(live.dimPercent) },
                    range = LauncherSettings.MIN_DIM.toFloat()..LauncherSettings.MAX_DIM.toFloat(),
                    modifier = Modifier.weight(1f).testTag(TestTags.DIM_SLIDER),
                )
                Text("${live.dimPercent}%", color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp, modifier = Modifier.width(40.dp))
            }
        }
        Section("Clock style") {
            ChoiceRow {
                ThemePresets.clockStyles.forEach { style ->
                    Choice(clockStyleLabel(style), settings.clockStyle == style, TestTags.clockStyle(style.name)) { actions.onClockStyle(style) }
                }
            }
            if (settings.clockStyle == ClockStyle.ORNAMENT) {
                Spacer(Modifier.height(4.dp))
                ChoiceRow {
                    Ornament.entries.forEach { o ->
                        Choice(ornamentLabel(o), settings.ornament == o, TestTags.ornament(o.name)) { actions.onOrnament(o) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Time only", !settings.ornamentDate, TestTags.ORNAMENT_DATE + "_off") { actions.onOrnamentDate(false) }
                    Choice("Time and date", settings.ornamentDate, TestTags.ORNAMENT_DATE + "_on") { actions.onOrnamentDate(true) }
                }
                Hint("Thin digits with a decorative line. Pairs well with the Cormorant or Playfair fonts.")
            }
        }
        Section("Layout") {
            ChoiceRow {
                HomeLayout.entries.forEach { l ->
                    Choice(layoutLabel(l), settings.layout == l, "layout_${l.name}") { actions.onLayout(l) }
                }
            }
        }
        Section("Backdrop") {
            ChoiceRow {
                Backdrop.entries.forEach { b ->
                    Choice(backdropLabel(b), settings.backdrop == b, "backdrop_option_${b.name}") { actions.onBackdrop(b) }
                }
            }
        }
        Section("Colours") {
            ChoiceRow {
                ThemePresets.palettes.forEach { p ->
                    PaletteTile(p.name, p.backgroundTop, p.backgroundBottom, p.accent, settings.paletteId == p.id, TestTags.palette(p.id)) {
                        actions.onPalette(p.id)
                    }
                }
            }
        }
        Section("Accent") {
            ChoiceRow {
                Choice("Palette", settings.accentArgb == null, TestTags.accent(null)) { actions.onAccent(null) }
                AccentSwatches.forEach { argb ->
                    Swatch(argb, settings.accentArgb == argb, TestTags.accent(argb)) { actions.onAccent(argb) }
                }
            }
            Spacer(Modifier.height(8.dp))
            HueSlider(live, actions, onAccentDrag)
        }
        Section("Font") {
            ChoiceRow {
                ThemePresets.fonts.forEach { font ->
                    val family = remember(font.id) { fontFamilyFor(font) }
                    Choice(font.name, settings.fontId == font.id, TestTags.font(font.id), family = family) { actions.onFont(font.id) }
                }
            }
        }
        Section("App drawer") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Plain", settings.drawerStyle == DrawerStyle.PLAIN, TestTags.drawerStyle(DrawerStyle.PLAIN.name)) { actions.onDrawerStyle(DrawerStyle.PLAIN) }
                Choice("Match theme", settings.drawerStyle == DrawerStyle.THEMED, TestTags.drawerStyle(DrawerStyle.THEMED.name)) { actions.onDrawerStyle(DrawerStyle.THEMED) }
            }
            Hint("Plain keeps the drawer neutral and follows your phone's light or dark mode. Themes never change it unless you choose Match theme.")
        }
        Section("App icons") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Original", settings.iconStyle == IconStyle.ORIGINAL, TestTags.iconStyle(IconStyle.ORIGINAL.name)) { actions.onIconStyle(IconStyle.ORIGINAL) }
                Choice("Tint to theme", settings.iconStyle == IconStyle.THEMED, TestTags.iconStyle(IconStyle.THEMED.name)) { actions.onIconStyle(IconStyle.THEMED) }
            }
            Hint("Original shows every app's own icon. Tint uses the app's Android 13 themed icon where it has one, and a tinted copy otherwise.")
        }
        Section("About") {
            Hint("Fonts: Inter, Space Grotesk, Outfit, Roboto Mono, Cormorant Garamond, Playfair Display, Josefin Sans and Syne, licensed under the SIL Open Font License 1.1. Licence texts are bundled in the app's assets/licenses folder.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------- Lock screen

@Composable
private fun LockTab(
    settings: LauncherSettings,
    actions: CustomiseActions,
    lockWallpaperStatus: String?,
    onBrowseThemes: () -> Unit,
    onRequestLockWallpaper: () -> Unit,
) {
    val colors = LauncherTheme.colors
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val previewW = if (wide) min(maxWidth / 4, 220.dp) else min(maxWidth * 0.42f, 150.dp)
        val controls: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Section("Lock screen look") {
                    Text(lockThemeLabel(settings), color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 14.sp)
                    ChoiceRow {
                        Choice("Browse themes", false, TestTags.BROWSE_THEMES) { onBrowseThemes() }
                        if (settings.lockThemeId != null) {
                            Choice("Match home screen", false, TestTags.LOCK_FOLLOW_HOME) { actions.onLockFollowsHome() }
                        }
                    }
                    Hint("Open a theme and choose Lock screen to give the lock screen its own look.")
                }
                Section("Themed lock screen") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice("Off", !settings.lockStyleEnabled, TestTags.LOCK_TOGGLE + "_off") { actions.onLockStyle(false) }
                        Choice("On", settings.lockStyleEnabled, TestTags.LOCK_TOGGLE + "_on") { actions.onLockStyle(true) }
                        Choice("Preview", false, TestTags.PREVIEW_LOCK) { actions.onPreviewLock() }
                    }
                    Hint(
                        "Shows over the system lock screen while your phone is locked, and goes away as soon as you unlock " +
                            "with your PIN, fingerprint or face. Your phone's security is unchanged.",
                    )
                }
                Section("Phone lock-screen wallpaper") {
                    Choice("Set as lock-screen wallpaper", false, TestTags.SET_LOCK_WALLPAPER) { onRequestLockWallpaper() }
                    Hint(lockWallpaperStatus ?: "Puts this lock screen's background on your phone's real lock screen. The phone's own clock stays on top.")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                LockPreview(settings, previewW, border = colors.border)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    LockPreview(settings, previewW, border = colors.border)
                }
                controls()
            }
        }
    }
}

// ---------------------------------------------------------------- Widgets

@Composable
private fun WidgetsTab(actions: CustomiseActions, widgetStatus: String?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Hint("Clock widgets you can place on any home screen that supports widgets. They keep time on their own, so they never wake your phone to update.")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardW = if (maxWidth >= 600.dp) (maxWidth - 16.dp) / 2 else maxWidth
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ClockWidgetKind.entries.forEach { kind ->
                    WidgetCard(kind, Modifier.width(cardW)) { actions.onAddWidget(kind) }
                }
            }
        }
        widgetStatus?.let { Hint(it) }
        Hint("Using Versity as your home app? Its own home screen shows the clock you pick in Style, including the Ornament styles. Placing widgets directly on Versity's home screen is coming next.")
    }
}

private val WidgetLook = LauncherSettings.DEFAULT.copy(paletteId = "abyss", fontId = "cormorant", backdrop = Backdrop.STARS, dimPercent = 0)

@Composable
private fun WidgetCard(kind: ClockWidgetKind, modifier: Modifier, onAdd: () -> Unit) {
    val colors = LauncherTheme.colors
    Column(modifier.border(1.dp, colors.border, Corner).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppTheme(WidgetLook) {
            val wc = LauncherTheme.colors
            Box(
                Modifier.fillMaxWidth().height(140.dp)
                    .background(Brush.verticalGradient(listOf(wc.backgroundTop, wc.backgroundBottom)), Corner),
                contentAlignment = Alignment.Center,
            ) {
                BackdropLayer(Backdrop.STARS, ThemePresets.palette("abyss"), wc.accent, Modifier.fillMaxSize())
                OrnamentClock(
                    PreviewTime, false, Ornament.CELESTIAL, Modifier.fillMaxWidth(),
                    date = PreviewDate.takeIf { kind.showsDate }, sizeRange = 40f..52f,
                )
            }
        }
        Text(kind.title, color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(kind.description, color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)
        PrimaryButton("Add to home screen", Modifier.testTag(TestTags.addWidget(kind.name)), onAdd)
    }
}

// ---------------------------------------------------------------- Shared pieces

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(title)
        content()
    }
}

@Composable
private fun Label(text: String) =
    Text(text.uppercase(), color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 11.sp, letterSpacing = 0.8.sp)

@Composable
private fun Hint(text: String) =
    Text(text, color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp, lineHeight = 16.sp)

@Composable
private fun ChoiceRow(content: @Composable () -> Unit) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }

@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    family: FontFamily? = null,
    onClick: () -> Unit,
) {
    val colors = LauncherTheme.colors
    Box(
        modifier
            .testTag(tag)
            .heightIn(min = 40.dp)
            .border(1.dp, if (selected) colors.accent else colors.border, Corner)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics { this.selected = selected; role = Role.RadioButton },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) colors.accent else colors.onSurface,
            fontFamily = family ?: LauncherTheme.font,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

/** Thin square checkbox, label and optional hint; the whole row is the touch target. */
@Composable
private fun CheckRow(label: String, hint: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val colors = LauncherTheme.colors
    Row(
        Modifier
            .testTag(tag)
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable { onChange(!checked) }
            .semantics(mergeDescendants = true) { role = Role.Checkbox; stateDescription = if (checked) "On" else "Off" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(18.dp).border(1.dp, if (checked) colors.accent else colors.muted, Corner).padding(4.dp)
                .background(if (checked) colors.accent else Color.Transparent),
        )
        Column(Modifier.weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 13.sp)
            if (hint != null) Text(hint, color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun PrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Box(
        modifier
            .heightIn(min = 40.dp)
            .background(colors.accent, Corner)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = colors.backgroundBottom, fontFamily = LauncherTheme.font, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun TextButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 40.dp).clickable(onClick = onClick).padding(horizontal = 14.dp).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontFamily = LauncherTheme.font, fontSize = 13.sp) }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Box(
        Modifier.size(40.dp).clickable(onClick = onClick).semantics { contentDescription = "Close"; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(12.dp)) {
            val sw = 1.5.dp.toPx()
            drawLine(colors.muted, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = sw)
            drawLine(colors.muted, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = sw)
        }
    }
}

/** Plain rectangular confirmation (Material's AlertDialog has a 28dp shape that clashes with the rest of the UI). */
@Composable
private fun LockWallpaperDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = LauncherTheme.colors
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.fillMaxWidth().background(colors.surface, Corner).border(1.dp, colors.border, Corner).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Replace your lock-screen wallpaper?", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                "Your current lock-screen wallpaper will be replaced with this lock screen's background. " +
                    "Your phone's own clock and unlock screen stay on top. Your home screen is not changed.",
                color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 13.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
                DialogAction("Cancel", TestTags.LOCK_WALLPAPER_CANCEL, colors.muted, onCancel)
                DialogAction("Replace", TestTags.LOCK_WALLPAPER_CONFIRM, colors.accent, onConfirm)
            }
        }
    }
}

@Composable
private fun DialogAction(label: String, tag: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.testTag(tag).heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 14.dp).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontFamily = LauncherTheme.font, fontSize = 13.sp) }
}

@Composable
private fun PaletteTile(name: String, top: Long, bottom: Long, accent: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Column(
        Modifier
            .testTag(tag)
            .width(84.dp)
            .border(1.dp, if (selected) colors.accent else colors.border, Corner)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .semantics { this.selected = selected; role = Role.RadioButton; contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(36.dp).background(Brush.verticalGradient(listOf(Color(top), Color(bottom))))) {
            Box(Modifier.align(Alignment.BottomStart).padding(4.dp).size(width = 16.dp, height = 3.dp).background(Color(accent)))
        }
        Text(name, color = if (selected) colors.accent else colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun Swatch(argb: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Box(
        Modifier
            .testTag(tag)
            .size(40.dp)
            .border(1.dp, if (selected) colors.onSurface else colors.border, Corner)
            .padding(5.dp)
            .background(Color(argb))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected; role = Role.RadioButton; contentDescription = "Accent colour" },
    )
}

@Composable
private fun HueSlider(settings: LauncherSettings, actions: CustomiseActions, onDrag: (Long?) -> Unit) {
    val current = settings.accentArgb ?: ThemePresets.palette(settings.paletteId).accent
    val hue = FloatArray(3).also { AndroidColor.colorToHSV(current.toInt(), it) }[0]
    val spectrum = remember { Brush.horizontalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 0.6f, 1f))) }) }
    Column {
        Text("Custom hue", color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)
        ThinSlider(
            value = hue,
            onChange = { h -> onDrag(AndroidColor.HSVToColor(floatArrayOf(h, 0.6f, 1f)).toLong() and 0xFFFFFFFFL) },
            onFinished = { actions.onAccent(settings.accentArgb) },
            range = 0f..359f,
            trackBrush = spectrum,
        )
    }
}

/** Slim slider: 2dp track, small rectangular thumb. Material's default is far heavier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackBrush: Brush? = null,
    onFinished: () -> Unit = {},
) {
    val colors = LauncherTheme.colors
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        valueRange = range,
        modifier = modifier,
        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent, inactiveTrackColor = colors.border),
        thumb = { Box(Modifier.size(width = 8.dp, height = 20.dp).background(colors.accent, RoundedCornerShape(1.dp))) },
        track = { state ->
            val frac = (state.value - range.start) / (range.endInclusive - range.start)
            if (trackBrush != null) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(trackBrush))
            } else {
                Box(Modifier.fillMaxWidth().height(2.dp).background(colors.border)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(colors.accent))
                }
            }
        },
    )
}
