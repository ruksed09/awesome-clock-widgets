package com.versity.launcher.ui.settings

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.IconStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.Ornament
import com.versity.launcher.data.UiMode
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
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCard
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioCheckRow
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioCorner
import com.versity.launcher.ui.studio.StudioDivider
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioIconButton
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioMotion
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioSegmented
import com.versity.launcher.ui.studio.StudioTag
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioTheme
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.ThemeToggleIcon
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
    fun onUiMode(mode: UiMode) {}
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

private val PreviewTime: LocalTime = LocalTime.of(11, 11)
private val PreviewDate: LocalDate = LocalDate.of(2026, 11, 11)
private val AccentSwatches = listOf(
    0xFF8FA3FFL, 0xFF4FC3E8L, 0xFF5ED6A0L, 0xFFFFD166L,
    0xFFFF8A5CL, 0xFFFF6B8BL, 0xFFC792EAL, 0xFFE8EAF2L,
)
private val Gutter = 16.dp

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

private fun uiModeLabel(m: UiMode) = when (m) {
    UiMode.SYSTEM -> "System"
    UiMode.LIGHT -> "Light"
    UiMode.DARK -> "Dark"
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
) = StudioTheme(settings.uiMode) {
    val c = Studio.colors
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var sheetThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val sheetTheme = sheetThemeId?.let(ThemePresets::theme)

    Box(modifier.fillMaxSize().background(c.page)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Header(
                dark = c.dark,
                onBack = onBack,
                onReset = { showResetDialog = true },
                onToggleTheme = { actions.onUiMode(if (c.dark) UiMode.LIGHT else UiMode.DARK) },
            )
            Tabs(tab) { tab = it }
            Crossfade(tab, animationSpec = tween(StudioMotion.QUICK), label = "tab") { current ->
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
        ApplySheetHost(sheetTheme, settings, onDismiss = { sheetThemeId = null }) { request ->
            sheetThemeId = null
            actions.onApplyTheme(request)
        }
    }
    if (showLockDialog) {
        ConfirmDialog(
            title = "Replace lock-screen wallpaper?",
            body = "Your current lock-screen wallpaper will be replaced with this background. Your phone's clock and unlock screen stay on top. Your home screen doesn't change.",
            cancel = "Keep current", confirm = "Replace wallpaper",
            cancelTag = TestTags.LOCK_WALLPAPER_CANCEL, confirmTag = TestTags.LOCK_WALLPAPER_CONFIRM,
            onConfirm = { showLockDialog = false; actions.onSetLockWallpaper() },
            onCancel = { showLockDialog = false },
        )
    }
    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset everything?",
            body = "This returns Versity to its original look and clears your photo, home screen apps, lock screen and drawer choices. Your phone's own wallpapers aren't touched.",
            cancel = "Keep settings", confirm = "Reset",
            cancelTag = TestTags.RESET_CANCEL, confirmTag = TestTags.RESET_CONFIRM,
            onConfirm = { showResetDialog = false; actions.onReset() },
            onCancel = { showResetDialog = false },
        )
    }
}

/** Slim 52dp bar: back, title, reset and the light/dark toggle. */
@Composable
private fun Header(dark: Boolean, onBack: () -> Unit, onReset: () -> Unit, onToggleTheme: () -> Unit) {
    val c = Studio.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clickable(onClick = onBack).semantics { contentDescription = "Back"; role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val w = size.width
                    val sw = 1.6.dp.toPx()
                    drawLine(c.ink, Offset(w * 0.62f, w * 0.18f), Offset(w * 0.3f, w * 0.5f), sw, StrokeCap.Round)
                    drawLine(c.ink, Offset(w * 0.3f, w * 0.5f), Offset(w * 0.62f, w * 0.82f), sw, StrokeCap.Round)
                }
            }
            StudioText("Customise", StudioType.title, modifier = Modifier.weight(1f))
            StudioTextButton("Reset", Modifier.testTag(TestTags.RESET), onClick = onReset)
            StudioIconButton(
                if (dark) "Switch to light mode" else "Switch to dark mode",
                Modifier.testTag(TestTags.THEME_TOGGLE),
                onClick = onToggleTheme,
            ) { ThemeToggleIcon(dark) }
        }
        StudioDivider()
    }
}

/** Text tabs with an indicator that slides to the selected tab. */
@Composable
private fun Tabs(selected: CustomiseTab, onSelect: (CustomiseTab) -> Unit) {
    val c = Studio.colors
    val density = LocalDensity.current
    val spans = remember { mutableStateMapOf<CustomiseTab, Pair<Dp, Dp>>() }
    val span = spans[selected]
    val x by animateDpAsState(span?.first ?: 0.dp, tween(300, easing = StudioMotion.Signature), label = "tabX")
    val w by animateDpAsState(span?.second ?: 0.dp, tween(300, easing = StudioMotion.Signature), label = "tabW")
    Column(Modifier.background(c.page)) {
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Row(Modifier.padding(horizontal = 8.dp)) {
                CustomiseTab.entries.forEach { t ->
                    val on = t == selected
                    Box(
                        Modifier
                            .testTag(TestTags.tab(t.name))
                            .heightIn(min = 44.dp)
                            .onPlaced { p ->
                                with(density) { spans[t] = p.positionInParent().x.toDp() to p.size.width.toDp() }
                            }
                            .selectable(selected = on, role = Role.Tab) { onSelect(t) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StudioText(t.label, if (on) StudioType.bodyStrong else StudioType.body, if (on) c.ink else c.muted, maxLines = 1)
                    }
                }
            }
            if (span != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 8.dp + x + 12.dp)
                        .width((w - 24.dp).coerceAtLeast(0.dp))
                        .height(2.dp)
                        .background(c.brand, RoundedCornerShape(1.dp)),
                )
            }
        }
        StudioDivider()
    }
}

// ---------------------------------------------------------------- Themes

@Composable
private fun ThemesTab(settings: LauncherSettings, onOpen: (ThemeBundle) -> Unit) {
    var category by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    val list = remember(category) { ThemePresets.themes.filter { category == null || it.category == category } }
    val home = currentHomeTheme(settings)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppliedSummary(home?.name ?: "Custom mix", lockThemeLabel(settings))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StudioChip("All", category == null, TestTags.category("ALL")) { category = null }
                    ThemeCategory.entries.forEach { cat ->
                        StudioChip(cat.label, category == cat, TestTags.category(cat.name)) { category = cat }
                    }
                }
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

/** What is applied where, at a glance. */
@Composable
private fun AppliedSummary(home: String, lock: String) {
    val c = Studio.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface, StudioCardCorner)
            .border(1.dp, c.line, StudioCardCorner),
    ) {
        listOf("Home screen" to home, "Lock screen" to lock).forEachIndexed { i, (label, value) ->
            if (i > 0) Box(Modifier.width(1.dp).height(52.dp).background(c.line))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StudioText(label, StudioType.caption, c.muted, maxLines = 1)
                StudioText(value, StudioType.bodyStrong, maxLines = 1)
            }
        }
    }
}

/** Gallery card: the real home screen in this theme, scaled down, then its name and tagline. */
@Composable
private fun ThemeCard(theme: ThemeBundle, onHome: Boolean, onLock: Boolean, onClick: () -> Unit) {
    val c = Studio.colors
    val applied = onHome || onLock
    val look = remember(theme.id) { LauncherSettings.DEFAULT.withTheme(theme) }
    val where = listOfNotNull("home screen".takeIf { onHome }, "lock screen".takeIf { onLock })
    Column(
        Modifier
            .testTag(TestTags.theme(theme.id))
            .clip(StudioCardCorner)
            .background(c.surface)
            .border(1.dp, if (applied) c.brand else c.line, StudioCardCorner)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = applied
                if (where.isNotEmpty()) stateDescription = "Applied to ${where.joinToString(" and ")}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            HomePreview(look, maxWidth, onClick = onClick)
        }
        Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioText(theme.name, StudioType.bodyStrong, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                if (onHome) StudioTag("Home")
                if (onLock) StudioTag("Lock")
            }
            StudioText(theme.tagline, StudioType.caption, c.muted, maxLines = 2)
        }
    }
}

/** Scrim plus bottom sheet; the sheet rises on a decelerating curve and leaves on an accelerating one. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ApplySheetHost(
    theme: ThemeBundle?,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (ThemeApplyRequest) -> Unit,
) {
    // Keep the last theme on screen while the sheet animates out.
    var shown by remember { mutableStateOf(theme) }
    if (theme != null) shown = theme
    AnimatedVisibility(visible = theme != null, enter = fadeIn(tween(1)), exit = fadeOut(tween(StudioMotion.QUICK + 50))) {
        val c = Studio.colors
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(200)))
                    .background(c.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = slideInVertically(tween(StudioMotion.SLOW, easing = StudioMotion.Enter)) { it / 3 } + fadeIn(tween(StudioMotion.STANDARD)),
                        exit = slideOutVertically(tween(200, easing = StudioMotion.Exit)) { it / 4 } + fadeOut(tween(200)),
                    ),
            ) {
                shown?.let { ThemeApplySheet(it, settings, onDismiss, onApply) }
            }
        }
    }
}

/** Bottom sheet: home and lock previews, where to apply, and what else to change on the phone. */
@Composable
private fun ThemeApplySheet(
    theme: ThemeBundle,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (ThemeApplyRequest) -> Unit,
) {
    val c = Studio.colors
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
    val lockChanges = target.lock || (target.home && settings.lockThemeId == null)
    val sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)

    Column(
        Modifier
            .testTag(TestTags.APPLY_SHEET)
            .semantics { paneTitle = "Apply ${theme.name}" }
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(sheetShape)
            .background(c.surface)
            .border(1.dp, c.line, sheetShape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            // Absorb taps so they never reach the scrim.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 32.dp, height = 4.dp).background(c.lineStrong, RoundedCornerShape(2.dp)))
        }
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StudioLabel(theme.category.label)
                    StudioText(theme.name, StudioType.heading)
                    StudioText(theme.tagline, StudioType.small, c.muted)
                }
                Box(
                    Modifier.size(48.dp).clickable(onClick = onDismiss).semantics { contentDescription = "Close"; role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        val sw = 1.6.dp.toPx()
                        drawLine(c.muted, Offset(0f, 0f), Offset(size.width, size.height), sw, StrokeCap.Round)
                        drawLine(c.muted, Offset(size.width, 0f), Offset(0f, size.height), sw, StrokeCap.Round)
                    }
                }
            }
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PreviewStage {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val each: Dp = min((maxWidth - 12.dp) / 2, 128.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                            LabelledPreview("Home screen", changed = target.home) { HomePreview(homeAfter, each, border = c.line) }
                            LabelledPreview("Lock screen", changed = lockChanges) { LockPreview(lockAfter, each, border = c.line) }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StudioLabel("Apply to")
                    StudioSegmented(
                        ApplyTarget.entries, target, { it.label }, { target = it },
                        tag = { TestTags.applyTarget(it.name) },
                    )
                }
                Column {
                    if (target.home && hasPhoto) {
                        StudioCheckRow("Use the theme background instead of my photo", null, useThemeBackground, TestTags.OPT_THEME_BACKGROUND) { useThemeBackground = it }
                    }
                    if (target.home) {
                        StudioCheckRow("Also set as phone wallpaper", "Shows behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
                    }
                    if (target.lock) {
                        StudioCheckRow(
                            "Show themed lock screen while locked",
                            "Appears over your lock screen. Unlocking still needs your PIN, fingerprint or face.",
                            showLockScreen, TestTags.OPT_LOCK_SCREEN,
                        ) { showLockScreen = it }
                        StudioCheckRow(
                            "Also set as lock-screen wallpaper",
                            "Replaces your current one. Your phone's clock stays on top.",
                            setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER,
                        ) { setLockWallpaper = it }
                    }
                }
            }
        }
        StudioDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioTextButton("Cancel", Modifier.testTag(TestTags.APPLY_CANCEL), onClick = onDismiss)
            StudioPrimaryButton("Apply theme", Modifier.testTag(TestTags.APPLY_CONFIRM)) {
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

/** Recessed well that previews sit in, so phone frames read as objects on a stage. */
@Composable
private fun PreviewStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Studio.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(c.panel, StudioCardCorner)
            .border(1.dp, c.line, StudioCardCorner)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun LabelledPreview(label: String, changed: Boolean, content: @Composable () -> Unit) {
    val c = Studio.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            StudioText(label, StudioType.caption.copy(fontWeight = FontWeight.Medium), if (changed) c.ink else c.muted, maxLines = 1)
            if (!changed) StudioText("· no change", StudioType.caption, c.muted, maxLines = 1)
        }
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
    val c = Studio.colors

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val controls: @Composable () -> Unit = { StyleControls(settings, actions, live, { dragDim = it }, { dragAccent = it }) }
        when {
            w >= 720.dp -> Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(1f).padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
                    PreviewStage { Previews(live, min((w / 2 - 80.dp) / 2, (h - 96.dp) * (360f / 780f))) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) { controls() }
            }
            // Tall phones keep the previews pinned while the settings scroll underneath.
            h >= 600.dp -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().background(c.panel).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Previews(live, min((w - 56.dp) / 2, h * 0.28f * (360f / 780f)))
                }
                StudioDivider()
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 16.dp)) { controls() }
            }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 16.dp)) {
                PreviewStage { Previews(live, min((w - 80.dp) / 2, 120.dp)) }
                Spacer(Modifier.height(16.dp))
                controls()
            }
        }
    }
}

/** Home and lock side by side, [each] wide. Tagged so tests can read which palette is previewed. */
@Composable
private fun Previews(settings: LauncherSettings, each: Dp) {
    val c = Studio.colors
    Row(
        Modifier.testTag(TestTags.PREVIEW).semantics { previewPalette = settings.paletteId },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LabelledPreview("Home screen", changed = true) { HomePreview(settings, each, border = c.line) }
        LabelledPreview("Lock screen", changed = true) { LockPreview(settings, each, border = c.line) }
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
    val c = Studio.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioCard(title = "Background") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudioButton(if (settings.wallpaperUri == null) "Choose photo" else "Change photo", Modifier.testTag(TestTags.PICK_WALLPAPER)) { actions.onPickWallpaper() }
                if (settings.wallpaperUri != null) {
                    StudioTextButton("Remove photo", Modifier.testTag(TestTags.CLEAR_WALLPAPER)) { actions.onClearWallpaper() }
                }
            }
            StudioHint(if (settings.wallpaperUri == null) "Using the theme's background." else "Using your photo. Themes keep it unless you choose otherwise.")
            StudioDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioText("Dim", StudioType.body, modifier = Modifier.width(44.dp))
                ThinSlider(
                    value = live.dimPercent.toFloat(),
                    onChange = { onDimDrag(it.toInt()) },
                    onFinished = { actions.onDim(live.dimPercent) },
                    range = LauncherSettings.MIN_DIM.toFloat()..LauncherSettings.MAX_DIM.toFloat(),
                    modifier = Modifier.weight(1f).testTag(TestTags.DIM_SLIDER),
                    description = "Dim, ${live.dimPercent} percent",
                )
                StudioText("${live.dimPercent}%", StudioType.small, c.muted, Modifier.width(40.dp).padding(start = 8.dp))
            }
        }
        StudioCard(title = "Clock") {
            ChoiceRow {
                ThemePresets.clockStyles.forEach { style ->
                    StudioChip(clockStyleLabel(style), settings.clockStyle == style, TestTags.clockStyle(style.name)) { actions.onClockStyle(style) }
                }
            }
            if (settings.clockStyle == ClockStyle.ORNAMENT) {
                StudioDivider()
                StudioLabel("Ornament")
                ChoiceRow {
                    Ornament.entries.forEach { o ->
                        StudioChip(ornamentLabel(o), settings.ornament == o, TestTags.ornament(o.name)) { actions.onOrnament(o) }
                    }
                }
                StudioSegmented(
                    listOf(false, true), settings.ornamentDate, { if (it) "Time and date" else "Time only" }, { actions.onOrnamentDate(it) },
                    tag = { TestTags.ORNAMENT_DATE + if (it) "_on" else "_off" },
                )
            }
        }
        StudioCard(title = "Layout") {
            StudioSegmented(HomeLayout.entries, settings.layout, ::layoutLabel, { actions.onLayout(it) }, tag = { "layout_${it.name}" })
        }
        StudioCard(title = "Backdrop") {
            ChoiceRow {
                Backdrop.entries.forEach { b ->
                    StudioChip(backdropLabel(b), settings.backdrop == b, "backdrop_option_${b.name}") { actions.onBackdrop(b) }
                }
            }
        }
        StudioCard(title = "Colours") {
            ChoiceRow {
                ThemePresets.palettes.forEach { p ->
                    PaletteTile(p.name, p.backgroundTop, p.backgroundBottom, p.accent, settings.paletteId == p.id, TestTags.palette(p.id)) {
                        actions.onPalette(p.id)
                    }
                }
            }
            StudioDivider()
            StudioLabel("Accent")
            ChoiceRow {
                StudioChip("From palette", settings.accentArgb == null, TestTags.accent(null)) { actions.onAccent(null) }
                AccentSwatches.forEach { argb ->
                    Swatch(argb, settings.accentArgb == argb, TestTags.accent(argb)) { actions.onAccent(argb) }
                }
            }
            HueSlider(live, actions, onAccentDrag)
        }
        StudioCard(title = "Font") {
            ChoiceRow {
                ThemePresets.fonts.forEach { font ->
                    val family = remember(font.id) { fontFamilyFor(font) }
                    StudioChip(font.name, settings.fontId == font.id, TestTags.font(font.id), family = family) { actions.onFont(font.id) }
                }
            }
        }
        StudioCard(title = "App drawer and icons") {
            StudioLabel("Drawer")
            StudioSegmented(
                DrawerStyle.entries, settings.drawerStyle,
                { if (it == DrawerStyle.PLAIN) "Plain" else "Match theme" }, { actions.onDrawerStyle(it) },
                tag = { TestTags.drawerStyle(it.name) },
            )
            StudioHint("Plain stays neutral and follows App appearance below. Themes never restyle it unless you choose Match theme.")
            StudioDivider()
            StudioLabel("Icons")
            StudioSegmented(
                IconStyle.entries, settings.iconStyle,
                { if (it == IconStyle.ORIGINAL) "Original" else "Tint to theme" }, { actions.onIconStyle(it) },
                tag = { TestTags.iconStyle(it.name) },
            )
            StudioHint("Original keeps every app's own icon. Tint uses the app's themed icon where it has one.")
        }
        StudioCard(title = "App appearance") {
            StudioSegmented(UiMode.entries, settings.uiMode, ::uiModeLabel, { actions.onUiMode(it) }, tag = { TestTags.uiMode(it.name) })
            StudioHint("Light or dark for Customise and the plain app drawer. System follows your phone.")
        }
        StudioCard(title = "About") {
            StudioHint("Fonts: Inter, Space Grotesk, Outfit, Roboto Mono, Cormorant Garamond, Playfair Display, Josefin Sans and Syne, under the SIL Open Font License 1.1. Licence texts are in the app's assets/licenses folder.")
        }
        Spacer(Modifier.height(16.dp))
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val previewW = if (wide) min(maxWidth / 4, 220.dp) else min(maxWidth * 0.4f, 140.dp)
        val controls: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioCard(title = "Lock screen look") {
                    StudioText(lockThemeLabel(settings), StudioType.body)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StudioButton("Browse themes", Modifier.testTag(TestTags.BROWSE_THEMES)) { onBrowseThemes() }
                        if (settings.lockThemeId != null) {
                            StudioTextButton("Match home screen", Modifier.testTag(TestTags.LOCK_FOLLOW_HOME), color = Studio.colors.brandText) { actions.onLockFollowsHome() }
                        }
                    }
                    StudioHint("Open a theme and choose Lock to give the lock screen its own look.")
                }
                StudioCard(title = "Themed lock screen") {
                    StudioSegmented(
                        listOf(false, true), settings.lockStyleEnabled, { if (it) "On" else "Off" }, { actions.onLockStyle(it) },
                        tag = { TestTags.LOCK_TOGGLE + if (it) "_on" else "_off" },
                    )
                    StudioHint("Shows over your lock screen while the phone is locked and closes when you unlock with your PIN, fingerprint or face. Your phone's security doesn't change.")
                    StudioButton("Preview", Modifier.testTag(TestTags.PREVIEW_LOCK)) { actions.onPreviewLock() }
                }
                StudioCard(title = "Lock-screen wallpaper") {
                    StudioHint(lockWallpaperStatus ?: "Puts this background on your phone's own lock screen. The phone's clock stays on top.")
                    StudioButton("Set lock-screen wallpaper", Modifier.testTag(TestTags.SET_LOCK_WALLPAPER)) { onRequestLockWallpaper() }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(1f)) { PreviewStage { LockPreview(settings, previewW, border = Studio.colors.line) } }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 16.dp)) {
                PreviewStage { LockPreview(settings, previewW, border = Studio.colors.line) }
                Spacer(Modifier.height(12.dp))
                controls()
            }
        }
    }
}

// ---------------------------------------------------------------- Widgets

@Composable
private fun WidgetsTab(actions: CustomiseActions, widgetStatus: String?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StudioHint("Clock widgets for any home screen that supports widgets. They keep time on their own, so they never wake your phone.")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardW = if (maxWidth >= 600.dp) (maxWidth - 12.dp) / 2 else maxWidth
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClockWidgetKind.entries.forEach { kind ->
                    WidgetCard(kind, Modifier.width(cardW)) { actions.onAddWidget(kind) }
                }
            }
        }
        widgetStatus?.let { StudioHint(it) }
        StudioHint("Using Versity as your home app? Its home screen shows the clock you pick in Style, including the Ornament styles. Widgets placed directly on Versity's home screen are coming next.")
    }
}

private val WidgetLook = LauncherSettings.DEFAULT.copy(paletteId = "abyss", fontId = "cormorant", backdrop = Backdrop.STARS, dimPercent = 0)

@Composable
private fun WidgetCard(kind: ClockWidgetKind, modifier: Modifier, onAdd: () -> Unit) {
    StudioCard(modifier) {
        AppTheme(WidgetLook) {
            val wc = LauncherTheme.colors
            Box(
                Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(wc.backgroundTop, wc.backgroundBottom))),
                contentAlignment = Alignment.Center,
            ) {
                BackdropLayer(Backdrop.STARS, ThemePresets.palette("abyss"), wc.accent, Modifier.fillMaxSize())
                OrnamentClock(
                    PreviewTime, false, Ornament.CELESTIAL, Modifier.fillMaxWidth(),
                    date = PreviewDate.takeIf { kind.showsDate }, sizeRange = 40f..50f,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StudioText(kind.title, StudioType.bodyStrong)
            StudioText(kind.description, StudioType.small, Studio.colors.muted)
        }
        StudioPrimaryButton("Add to home screen", Modifier.testTag(TestTags.addWidget(kind.name)), onAdd)
    }
}

// ---------------------------------------------------------------- Shared pieces

@Composable
private fun ChoiceRow(content: @Composable () -> Unit) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }

/** Rectangular confirmation in the studio style: a question, the consequence, and buttons that name the action. */
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    cancel: String,
    confirm: String,
    cancelTag: String,
    confirmTag: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        val c = Studio.colors
        Column(
            Modifier.fillMaxWidth().background(c.surface, StudioCardCorner).border(1.dp, c.line, StudioCardCorner).padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StudioText(title, StudioType.title, modifier = Modifier.padding(end = 8.dp))
            StudioText(body, StudioType.body, c.muted, Modifier.padding(end = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                StudioTextButton(cancel, Modifier.testTag(cancelTag), onClick = onCancel)
                StudioPrimaryButton(confirm, Modifier.testTag(confirmTag), onConfirm)
            }
        }
    }
}

@Composable
private fun PaletteTile(name: String, top: Long, bottom: Long, accent: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val c = Studio.colors
    Column(
        Modifier
            .testTag(tag)
            .width(78.dp)
            .background(if (selected) c.brandSoft else c.surface, StudioCorner)
            .border(1.dp, if (selected) c.brand else c.line, StudioCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(5.dp)
            .semantics { contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(Color(top), Color(bottom)))),
        ) {
            Box(Modifier.align(Alignment.BottomStart).padding(4.dp).size(width = 14.dp, height = 3.dp).background(Color(accent)))
        }
        StudioText(name, StudioType.caption.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal), if (selected) c.brandText else c.ink, maxLines = 1)
    }
}

@Composable
private fun Swatch(argb: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val c = Studio.colors
    Box(
        Modifier
            .testTag(tag)
            .size(36.dp)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.ink else c.line, StudioCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(5.dp)
            .background(Color(argb), RoundedCornerShape(3.dp))
            .semantics { contentDescription = "Accent colour" },
    )
}

@Composable
private fun HueSlider(settings: LauncherSettings, actions: CustomiseActions, onDrag: (Long?) -> Unit) {
    val current = settings.accentArgb ?: ThemePresets.palette(settings.paletteId).accent
    val hue = FloatArray(3).also { AndroidColor.colorToHSV(current.toInt(), it) }[0]
    val spectrum = remember { Brush.horizontalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 0.6f, 1f))) }) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StudioText("Hue", StudioType.body, modifier = Modifier.width(44.dp))
        ThinSlider(
            value = hue,
            onChange = { h -> onDrag(AndroidColor.HSVToColor(floatArrayOf(h, 0.6f, 1f)).toLong() and 0xFFFFFFFFL) },
            onFinished = { actions.onAccent(settings.accentArgb) },
            range = 0f..359f,
            trackBrush = spectrum,
            modifier = Modifier.weight(1f),
            description = "Custom accent hue",
        )
    }
}

/** Slim slider: 2dp track, small rectangular thumb, 48dp touch height. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackBrush: Brush? = null,
    onFinished: () -> Unit = {},
    description: String? = null,
) {
    val c = Studio.colors
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        valueRange = range,
        modifier = modifier.heightIn(min = 48.dp).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        colors = SliderDefaults.colors(thumbColor = c.brand, activeTrackColor = c.brand, inactiveTrackColor = c.line),
        thumb = {
            Box(
                Modifier.size(width = 10.dp, height = 20.dp)
                    .background(c.surface, RoundedCornerShape(3.dp))
                    .border(1.5.dp, c.brand, RoundedCornerShape(3.dp)),
            )
        },
        track = { state ->
            val frac = (state.value - range.start) / (range.endInclusive - range.start)
            if (trackBrush != null) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(trackBrush))
            } else {
                Box(Modifier.fillMaxWidth().height(2.dp).background(c.line, RoundedCornerShape(1.dp))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(c.brand, RoundedCornerShape(1.dp)))
                }
            }
        },
    )
}
