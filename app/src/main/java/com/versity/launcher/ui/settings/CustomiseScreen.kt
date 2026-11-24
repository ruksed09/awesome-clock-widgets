package com.versity.launcher.ui.settings

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.versity.launcher.R
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockPlacement
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
import com.versity.launcher.domain.WallpaperOption
import com.versity.launcher.domain.Wallpapers
import com.versity.launcher.domain.isLight
import com.versity.launcher.domain.lockLook
import com.versity.launcher.domain.matches
import com.versity.launcher.domain.withTheme
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.HomePreview
import com.versity.launcher.ui.home.components.BigClockStyles
import com.versity.launcher.ui.components.LockPreview
import com.versity.launcher.ui.previewPalette
import com.versity.launcher.ui.studio.BackGlyph
import com.versity.launcher.ui.studio.ClayTile
import com.versity.launcher.ui.studio.GlassBackdrop
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCard
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioCheckRow
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioChipCorner
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
import com.versity.launcher.ui.studio.glass
import com.versity.launcher.ui.theme.fontFamilyFor
import com.versity.launcher.widget.ClockWidgetKind
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.lock.LockStyleContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import kotlin.math.roundToInt

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
    fun onClockPlacement(home: ClockPlacement?, lock: ClockPlacement?) {}
    fun onApplyTheme(request: ThemeApplyRequest) {}
    fun onApplyWallpaper(request: WallpaperApplyRequest) {}
    fun onLockFollowsHome() {}
    fun onAddWidget(kind: ClockWidgetKind) {}
    fun onPreviewLock() {}
    fun onSetLockWallpaper() {}
    fun onSetDefaultLauncher() {}
    fun onReset() {}
}

/** Where Customise is. HUB is the landing page; STYLE, LOCK and DRAWER sit under MORE. */
enum class CustomisePage(val title: String) {
    HUB("Versity"), THEMES("Themes"), WALLPAPERS("Wallpapers"), WIDGETS("Widgets"), MORE("More"),
    STYLE("Clock and style"), LOCK("Lock screen"), DRAWER("App drawer and icons");

    val parent: CustomisePage? get() = when (this) {
        HUB -> null
        STYLE, LOCK, DRAWER -> MORE
        else -> HUB
    }
}

enum class ApplyTarget(val label: String) {
    HOME("Home"), LOCK("Lock"), BOTH("Both");

    val home: Boolean get() = this != LOCK
    val lock: Boolean get() = this != HOME
}

/** Everything the theme sheet decided. Options that do not fit the target are false. */
data class ThemeApplyRequest(
    val theme: ThemeBundle,
    val target: ApplyTarget,
    /** Home: swap the current wallpaper for the theme's own (its wallpaper, or its gradient and backdrop). */
    val replaceWallpaper: Boolean,
    /** Home: also render the theme to the phone's home wallpaper (shown behind other apps). */
    val setPhoneWallpaper: Boolean,
    /** Lock: show the themed lock screen over the system lock screen while locked. */
    val showLockScreen: Boolean,
    /** Lock: also render the theme to the phone's lock-screen wallpaper. */
    val setLockWallpaper: Boolean,
)

/** Everything the wallpaper sheet decided. */
data class WallpaperApplyRequest(
    val wallpaper: WallpaperOption,
    val target: ApplyTarget,
    val setPhoneWallpaper: Boolean,
    val setLockWallpaper: Boolean,
    /** Palette to switch to so text stays readable on this wallpaper, or null to keep the current one. */
    val paletteId: String?,
)

private val PreviewTime = java.time.LocalTime.of(11, 11)
private val PreviewDate = java.time.LocalDate.of(2026, 11, 11)
private val AccentSwatches = listOf(
    0xFF8FA3FFL, 0xFF4FC3E8L, 0xFF5ED6A0L, 0xFFFFD166L,
    0xFFFF8A5CL, 0xFFFF6B8BL, 0xFFC792EAL, 0xFFE8EAF2L,
)
private val Gutter = 16.dp

/** Settings pages stay at a comfortable reading width on tablets and wide windows, centred. */
private fun Modifier.readableWidth(): Modifier =
    this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 720.dp)

private fun backdropLabel(b: Backdrop) = b.name.lowercase().replaceFirstChar { it.uppercase() }

private fun layoutLabel(l: HomeLayout) = when (l) {
    HomeLayout.CLASSIC -> "Classic"
    HomeLayout.MINIMAL -> "Minimal"
    HomeLayout.ORBIT -> "Centered"
}

private fun clockStyleLabel(s: ClockStyle) = when (s) {
    ClockStyle.STACKED -> "Stacked"
    ClockStyle.SINGLE_LINE -> "Single line"
    ClockStyle.ANALOGUE -> "Analogue"
    ClockStyle.ORNAMENT -> "Ornament"
    ClockStyle.GIANT -> "Giant"
    ClockStyle.OUTLINE -> "Outline"
    ClockStyle.NEON -> "Neon"
    ClockStyle.EXTRUDED -> "3D"
    ClockStyle.SEGMENT -> "LED"
    ClockStyle.FLIP -> "Flip"
    ClockStyle.STAGE -> "Stage"
    ClockStyle.GLASS -> "Glass"
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CustomiseContent(
    settings: LauncherSettings,
    actions: CustomiseActions,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    lockWallpaperStatus: String? = null,
    widgetStatus: String? = null,
    initialPage: CustomisePage = CustomisePage.HUB,
    isDefaultLauncher: Boolean = true,
) = StudioTheme(settings.uiMode) {
    val c = Studio.colors
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var forward by remember { mutableStateOf(true) }
    var sheetThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetWallpaperId by rememberSaveable { mutableStateOf<String?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var editingClock by rememberSaveable { mutableStateOf(false) }
    val go: (CustomisePage) -> Unit = { forward = true; page = it }
    val up: () -> Unit = { page.parent?.let { forward = false; page = it } ?: onBack() }
    BackHandler(enabled = page != CustomisePage.HUB) { up() }

    Box(modifier.fillMaxSize()) {
        GlassBackdrop(settings)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(300, easing = StudioMotion.Signature)) { it / 6 * dir } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(200, easing = StudioMotion.Exit)) { -it / 10 * dir } + fadeOut(tween(150)))
                },
                label = "page",
            ) { current ->
                Column(Modifier.fillMaxSize()) {
                    if (current == CustomisePage.HUB) {
                        HubHeader(c.dark) { actions.onUiMode(if (c.dark) UiMode.LIGHT else UiMode.DARK) }
                    } else {
                        PageHeader(
                            current.title, c.dark, onBack = up,
                            action = if (current == CustomisePage.STYLE) {
                                { StudioTextButton("Preview", Modifier.testTag(TestTags.PREVIEW_FAB), color = c.brandText) { previewing = true } }
                            } else null,
                        ) { actions.onUiMode(if (c.dark) UiMode.LIGHT else UiMode.DARK) }
                    }
                    when (current) {
                        CustomisePage.HUB -> HubPage(settings, isDefaultLauncher, go, actions::onSetDefaultLauncher)
                        CustomisePage.THEMES -> ThemesPage(settings) { sheetThemeId = it.id }
                        CustomisePage.WALLPAPERS -> WallpapersPage(settings, actions) { sheetWallpaperId = it.id }
                        CustomisePage.WIDGETS -> WidgetsPage(actions, widgetStatus)
                        CustomisePage.MORE -> MorePage(settings, actions, go, onReset = { showResetDialog = true })
                        CustomisePage.STYLE -> StylePage(settings, actions, previewing, onEditClock = { editingClock = true }) { previewing = it }
                        CustomisePage.LOCK -> LockPage(
                            settings, actions, lockWallpaperStatus,
                            onBrowseThemes = { go(CustomisePage.THEMES) },
                            onRequestLockWallpaper = { showLockDialog = true },
                        )
                        CustomisePage.DRAWER -> DrawerPage(settings, actions)
                    }
                }
            }
        }
        AnimatedVisibility(visible = editingClock, enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(StudioMotion.QUICK))) {
            ClockEditor(
                settings,
                onDone = { home, lock -> editingClock = false; actions.onClockPlacement(home, lock) },
                onCancel = { editingClock = false },
            )
        }
        val sheetTheme = sheetThemeId?.let(ThemePresets::theme)
        SheetHost(visible = sheetTheme != null, onDismiss = { sheetThemeId = null }, key = sheetTheme) { theme ->
            ThemeApplySheet(theme, settings, onDismiss = { sheetThemeId = null }) { request ->
                sheetThemeId = null
                actions.onApplyTheme(request)
            }
        }
        val sheetWallpaper = Wallpapers.byId(sheetWallpaperId)
        SheetHost(visible = sheetWallpaper != null, onDismiss = { sheetWallpaperId = null }, key = sheetWallpaper) { wp ->
            WallpaperApplySheet(wp, settings, onDismiss = { sheetWallpaperId = null }) { request ->
                sheetWallpaperId = null
                actions.onApplyWallpaper(request)
            }
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

// ---------------------------------------------------------------- Headers

@Composable
private fun HubHeader(dark: Boolean, onToggleTheme: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = Gutter, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ClayTile(Color(0xFF8475F2), Color(0xFF5141D2), 40.dp) { SparkleGlyph(Color.White, 18.dp) }
        Column(Modifier.weight(1f)) {
            StudioText("Versity", StudioType.display, modifier = Modifier.semantics { heading() })
            StudioText("Themes, wallpapers and widgets", StudioType.small, Studio.colors.muted, maxLines = 2)
        }
        ThemeToggle(dark, onToggleTheme)
    }
}

@Composable
private fun PageHeader(title: String, dark: Boolean, onBack: () -> Unit, action: (@Composable () -> Unit)? = null, onToggleTheme: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudioIconButton("Back", onClick = onBack) { BackGlyph() }
        StudioText(title, StudioType.title, modifier = Modifier.weight(1f).padding(start = 6.dp).semantics { heading() }, maxLines = 1)
        action?.invoke()
        ThemeToggle(dark, onToggleTheme)
    }
}

@Composable
private fun ThemeToggle(dark: Boolean, onToggle: () -> Unit) =
    StudioIconButton(
        if (dark) "Switch to light mode" else "Switch to dark mode",
        Modifier.testTag(TestTags.THEME_TOGGLE),
        onClick = onToggle,
    ) { ThemeToggleIcon(dark) }

// ---------------------------------------------------------------- Hub

private data class NavEntry(val page: CustomisePage, val title: String, val subtitle: String, val top: Color, val bottom: Color)

@Composable
private fun HubPage(settings: LauncherSettings, isDefault: Boolean, go: (CustomisePage) -> Unit, onSetDefault: () -> Unit) {
    val c = Studio.colors
    val home = currentHomeTheme(settings)
    val entries = listOf(
        NavEntry(CustomisePage.THEMES, "Themes", "${ThemePresets.themes.size} complete looks", Color(0xFF8C7BF5), Color(0xFF5845D8)),
        NavEntry(CustomisePage.WALLPAPERS, "Wallpapers", "${Wallpapers.all.size} wallpapers, or your photo", Color(0xFF5FC9D9), Color(0xFF2B8FB0)),
        NavEntry(CustomisePage.WIDGETS, "Widgets", "${ClockWidgetKind.entries.size} clock widgets", Color(0xFFF7C36B), Color(0xFFDA8A2E)),
        NavEntry(CustomisePage.MORE, "More", "Clock, lock screen, drawer", Color(0xFF9AA3B8), Color(0xFF5E6880)),
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Four across on tablets, two on phones, one on very narrow screens (folding phones' cover displays).
        val columns = when {
            maxWidth >= 720.dp -> 4
            maxWidth < 300.dp -> 1
            else -> 2
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 1000.dp)
                .padding(horizontal = Gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppliedSummary(home?.name ?: "Custom mix", lockThemeLabel(settings))
            entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { e -> NavCard(e, Modifier.weight(1f)) { go(e.page) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (!isDefault) {
                Row(
                    Modifier.fillMaxWidth().glass(c).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        StudioText("Use Versity as your home app", StudioType.bodyStrong)
                        StudioText("Themes and clocks show on your home screen.", StudioType.caption, c.muted)
                    }
                    StudioPrimaryButton("Set", Modifier.testTag(TestTags.DEFAULT_BANNER_SET), onSetDefault)
                }
            }
            StudioHint("Tip: long-press your home screen to come back here.", Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** Glass navigation card: clay icon, title, one line of detail and a small visual cue. */
@Composable
private fun NavCard(entry: NavEntry, modifier: Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.98f else 1f, tween(if (pressed) 90 else 150), label = "nav")
    Box(
        modifier
            .testTag(TestTags.nav(entry.page.name))
            .scale(scale)
            .height(184.dp)
            .glass(c)
            .clip(StudioCardCorner)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(14.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Narrow cards shrink the corner visual so it never runs into the text.
            val visualScale = (maxWidth / 170.dp).coerceIn(0.6f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayTile(entry.top, entry.bottom, 40.dp) { NavGlyph(entry.page) }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StudioText(entry.title, StudioType.cardTitle, maxLines = 1)
                    StudioText(entry.subtitle, StudioType.caption, c.muted, maxLines = 2)
                }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).graphicsLayer {
                    scaleX = visualScale; scaleY = visualScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                },
            ) { NavVisual(entry.page) }
        }
    }
}

@Composable
private fun NavGlyph(page: CustomisePage) {
    val ink = Color.White
    when (page) {
        CustomisePage.THEMES -> SparkleGlyph(ink, 18.dp)
        else -> Canvas(Modifier.size(18.dp)) {
            val w = size.width
            val sw = 1.8.dp.toPx()
            when (page) {
                CustomisePage.WALLPAPERS -> {
                    drawRoundRect(ink, Offset(w * 0.08f, w * 0.14f), Size(w * 0.84f, w * 0.72f), CornerRadius(w * 0.16f), style = Stroke(sw))
                    val m = Path().apply { moveTo(w * 0.16f, w * 0.78f); lineTo(w * 0.42f, w * 0.48f); lineTo(w * 0.6f, w * 0.66f); lineTo(w * 0.7f, w * 0.56f); lineTo(w * 0.86f, w * 0.74f) }
                    drawPath(m, ink, style = Stroke(sw, cap = StrokeCap.Round))
                    drawCircle(ink, w * 0.08f, Offset(w * 0.66f, w * 0.34f))
                }
                CustomisePage.WIDGETS -> listOf(0f to 0f, 0.54f to 0f, 0f to 0.54f, 0.54f to 0.54f).forEach { (x, y) ->
                    drawRoundRect(ink, Offset(w * (0.04f + x), w * (0.04f + y)), Size(w * 0.38f, w * 0.38f), CornerRadius(w * 0.1f), style = Stroke(sw))
                }
                else -> listOf(0.25f to 0.32f, 0.5f to 0.68f, 0.75f to 0.45f).forEach { (y, k) ->
                    drawLine(ink, Offset(w * 0.08f, w * y), Offset(w * 0.92f, w * y), sw, StrokeCap.Round)
                    drawCircle(ink, w * 0.1f, Offset(w * k, w * y))
                }
            }
        }
    }
}

/** Four-point star, the brand mark. */
@Composable
private fun SparkleGlyph(ink: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        val k = r * 0.18f
        val p = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
            quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
            quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
            quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
            close()
        }
        drawPath(p, ink)
    }
}

/** Small decorative cue in each hub card's corner: fanned thumbnails, a mini widget, setting chips. */
@Composable
private fun NavVisual(page: CustomisePage) {
    when (page) {
        CustomisePage.THEMES -> Fan(listOf(R.drawable.wp_glass_bloom_thumb, R.drawable.wp_dunes_thumb, R.drawable.wp_leviathan_thumb))
        CustomisePage.WALLPAPERS -> Fan(listOf(R.drawable.wp_silk_thumb, R.drawable.wp_pebbles_thumb, R.drawable.wp_moon_tide_thumb))
        CustomisePage.WIDGETS -> MiniWidget()
        else -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Clock", "Lock screen", "Drawer").forEach {
                StudioText(it, StudioType.caption, Studio.colors.muted, Modifier.glass(Studio.colors, RoundedCornerShape(8.dp), 2.dp).padding(horizontal = 8.dp, vertical = 2.dp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Fan(thumbs: List<Int>) {
    Box(Modifier.size(width = 92.dp, height = 76.dp)) {
        thumbs.forEachIndexed { i, res ->
            val angle = (i - 1) * 10f
            Image(
                painterResource(res), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = ((i - 1) * 22).dp, y = if (i == 1) (-6).dp else 0.dp)
                    .rotate(angle)
                    .size(width = 36.dp, height = 68.dp)
                    .shadow(6.dp, RoundedCornerShape(7.dp))
                    .clip(RoundedCornerShape(7.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(7.dp)),
            )
        }
    }
}

@Composable
private fun MiniWidget() {
    val serif = remember { fontFamilyFor(ThemePresets.font("cormorant")) }
    Box(
        Modifier.size(width = 96.dp, height = 58.dp).shadow(6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.wp_leviathan_thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("11:11", color = Color(0xFFDCEBFA), fontFamily = serif, fontSize = 20.sp, fontWeight = FontWeight.Light, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "lnum"))
            Canvas(Modifier.size(width = 54.dp, height = 6.dp)) {
                val y = size.height / 2
                drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xCCDCEBFA), Color.Transparent)), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                drawCircle(Color(0xFFDCEBFA), 1.6.dp.toPx(), center)
            }
        }
    }
}

/** What is applied where, at a glance. */
@Composable
private fun AppliedSummary(home: String, lock: String) {
    val c = Studio.colors
    Row(Modifier.fillMaxWidth().glass(c)) {
        listOf("Home screen" to home, "Lock screen" to lock).forEachIndexed { i, (label, value) ->
            if (i > 0) Box(Modifier.padding(vertical = 10.dp).width(1.dp).height(36.dp).background(c.line))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StudioText(label, StudioType.caption, c.muted, maxLines = 1)
                StudioText(value, StudioType.bodyStrong, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- Themes

@Composable
private fun ThemesPage(settings: LauncherSettings, onOpen: (ThemeBundle) -> Unit) {
    var category by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    val list = remember(category) { ThemePresets.themes.filter { category == null || it.category == category } }
    val home = currentHomeTheme(settings)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioChip("All", category == null, TestTags.category("ALL")) { category = null }
                ThemeCategory.entries.forEach { cat ->
                    StudioChip(cat.label, category == cat, TestTags.category(cat.name)) { category = cat }
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

/** Gallery card: the real home screen in this theme, scaled down, then its name and tagline. */
@Composable
private fun ThemeCard(theme: ThemeBundle, onHome: Boolean, onLock: Boolean, onClick: () -> Unit) {
    val c = Studio.colors
    val applied = onHome || onLock
    val look = remember(theme.id) { LauncherSettings.DEFAULT.withTheme(theme, useThemeWallpaper = true) }
    val where = listOfNotNull("home screen".takeIf { onHome }, "lock screen".takeIf { onLock })
    Column(
        Modifier
            .testTag(TestTags.theme(theme.id))
            .glass(c, elevation = 6.dp)
            .then(if (applied) Modifier.border(1.5.dp, c.brand, StudioCardCorner) else Modifier)
            .clip(StudioCardCorner)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = applied
                if (where.isNotEmpty()) stateDescription = "Applied to ${where.joinToString(" and ")}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))) {
            HomePreview(look, maxWidth, onClick = onClick)
            AppliedTags(onHome, onLock, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }
        Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StudioText(theme.name, StudioType.bodyStrong, maxLines = 1)
            StudioText(theme.tagline, StudioType.caption, c.muted, maxLines = 2)
        }
    }
}

// ---------------------------------------------------------------- Wallpapers

@Composable
private fun WallpapersPage(settings: LauncherSettings, actions: CustomiseActions, onOpen: (WallpaperOption) -> Unit) {
    val homeWp = Wallpapers.byUri(settings.wallpaperUri)
    val lockWp = Wallpapers.byUri(settings.lockLook().wallpaperUri)
    val photo = settings.wallpaperUri != null && homeWp == null
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "photo") {
            StudioCard(title = "Your photo") {
                StudioHint(if (photo) "Your photo is on the home screen." else "Use any photo from your phone as the wallpaper.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
                    StudioButton(if (photo) "Change photo" else "Choose photo", Modifier.testTag(TestTags.PICK_WALLPAPER)) { actions.onPickWallpaper() }
                    if (settings.wallpaperUri != null) {
                        StudioTextButton("Use no wallpaper", Modifier.testTag(TestTags.CLEAR_WALLPAPER)) { actions.onClearWallpaper() }
                    }
                }
            }
        }
        Wallpapers.collections.forEach { collection ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_$collection") {
                StudioLabel(collection, Modifier.padding(start = 4.dp, top = 8.dp))
            }
            items(Wallpapers.all.filter { it.collection == collection }, key = { it.id }) { wp ->
                WallpaperTile(wp, onHome = homeWp?.id == wp.id, onLock = lockWp?.id == wp.id) { onOpen(wp) }
            }
        }
    }
}

@Composable
private fun WallpaperTile(wp: WallpaperOption, onHome: Boolean, onLock: Boolean, onClick: () -> Unit) {
    val c = Studio.colors
    val applied = onHome || onLock
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .testTag(TestTags.wallpaper(wp.id))
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = wp.name
                selected = applied
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 19.5f)
                .shadow(8.dp, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                .clip(shape)
                .border(if (applied) 2.dp else 1.dp, if (applied) c.brand else Color.White.copy(alpha = 0.6f), shape),
        ) {
            Image(painterResource(wp.thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            AppliedTags(onHome, onLock, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }
        StudioText(wp.name, StudioType.small.copy(fontWeight = FontWeight.Medium), maxLines = 1, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

/** Small frosted "Home" / "Lock" chips on a preview's corner, marking where it is applied. */
@Composable
private fun AppliedTags(onHome: Boolean, onLock: Boolean, modifier: Modifier = Modifier) {
    if (!onHome && !onLock) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOfNotNull("Home".takeIf { onHome }, "Lock".takeIf { onLock }).forEach {
            Text(
                it, color = Color(0xFF1E1B2E), fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = Studio.font,
                modifier = Modifier.background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

/** Palette that keeps text readable on [wp]: light wallpapers need dark ink and vice versa. */
private fun readablePalette(settings: LauncherSettings, wp: WallpaperOption): String? {
    val currentLight = ThemePresets.palette(settings.paletteId).isLight()
    return when {
        wp.light && !currentLight -> "porcelain"
        !wp.light && currentLight -> "abyss"
        else -> null
    }
}

@Composable
private fun WallpaperApplySheet(
    wp: WallpaperOption,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (WallpaperApplyRequest) -> Unit,
) {
    var target by rememberSaveable(wp.id) { mutableStateOf(ApplyTarget.BOTH) }
    var setPhoneWallpaper by rememberSaveable(wp.id) { mutableStateOf(true) }
    var setLockWallpaper by rememberSaveable(wp.id) { mutableStateOf(true) }
    val suggested = readablePalette(settings, wp)
    var matchText by rememberSaveable(wp.id) { mutableStateOf(true) }
    val palette = suggested?.takeIf { matchText }

    val homeAfter = if (target.home) settings.copy(wallpaperUri = wp.uri, paletteId = palette ?: settings.paletteId) else settings
    val lockAfter = if (target.lock) settings.lockLook().copy(wallpaperUri = wp.uri, paletteId = palette ?: settings.lockLook().paletteId)
    else homeAfter.lockLook()

    SheetScaffold(
        label = wp.collection, title = wp.name, subtitle = "Wallpaper", tag = TestTags.WALLPAPER_SHEET,
        onDismiss = onDismiss,
        confirm = "Set wallpaper",
        onConfirm = {
            onApply(
                WallpaperApplyRequest(
                    wallpaper = wp, target = target,
                    setPhoneWallpaper = target.home && setPhoneWallpaper,
                    setLockWallpaper = target.lock && setLockWallpaper,
                    paletteId = palette,
                ),
            )
        },
    ) {
        SheetPreviews(homeAfter.copy(lockThemeId = null, lockWallpaperUri = null), lockAfter.copy(lockThemeId = null, lockWallpaperUri = null), target.home, target.lock || target.home)
        TargetPicker(target) { target = it }
        Column {
            if (target.home) StudioCheckRow("Also set as phone wallpaper", "Shows behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
            if (target.lock) StudioCheckRow("Also set as lock-screen wallpaper", "Replaces your current one. Your phone's clock stays on top.", setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER) { setLockWallpaper = it }
            if (suggested != null) {
                StudioCheckRow(
                    if (wp.light) "Use dark text for this light wallpaper" else "Use light text for this dark wallpaper",
                    "Keeps the clock and labels easy to read.", matchText, TestTags.OPT_MATCH_TEXT,
                ) { matchText = it }
            }
        }
    }
}

// ---------------------------------------------------------------- Sheets

/** Scrim plus bottom sheet; the sheet rises on a decelerating curve and leaves on an accelerating one. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun <T : Any> SheetHost(visible: Boolean, onDismiss: () -> Unit, key: T?, content: @Composable (T) -> Unit) {
    var shown by remember { mutableStateOf(key) }
    if (key != null) shown = key
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(1)), exit = fadeOut(tween(StudioMotion.QUICK + 50))) {
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
            ) { shown?.let { content(it) } }
        }
    }
}

/** Shared sheet layout: handle, title block, scrolling body and a pinned action bar. */
@Composable
private fun SheetScaffold(
    label: String,
    title: String,
    subtitle: String,
    tag: String,
    onDismiss: () -> Unit,
    confirm: String,
    onConfirm: () -> Unit,
    body: @Composable () -> Unit,
) {
    val c = Studio.colors
    BackHandler(onBack = onDismiss)
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Column(
        Modifier
            .testTag(tag)
            .semantics { paneTitle = title }
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .shadow(24.dp, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape)
            .background(c.surface.copy(alpha = 0.97f))
            .border(1.dp, Brush.verticalGradient(listOf(c.glassRimTop, Color.Transparent)), shape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).background(c.lineStrong, RoundedCornerShape(2.dp)))
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StudioLabel(label)
                    StudioText(title, StudioType.heading)
                    StudioText(subtitle, StudioType.small, c.muted)
                }
                StudioIconButton("Close", onClick = onDismiss) {
                    Canvas(Modifier.size(11.dp)) {
                        val sw = 1.6.dp.toPx()
                        drawLine(c.ink, Offset(0f, 0f), Offset(size.width, size.height), sw, StrokeCap.Round)
                        drawLine(c.ink, Offset(size.width, 0f), Offset(0f, size.height), sw, StrokeCap.Round)
                    }
                }
            }
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { body() }
        }
        StudioDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioTextButton("Cancel", Modifier.testTag(TestTags.APPLY_CANCEL), onClick = onDismiss)
            StudioPrimaryButton(confirm, Modifier.testTag(TestTags.APPLY_CONFIRM), onConfirm)
        }
    }
}

@Composable
private fun SheetPreviews(home: LauncherSettings, lock: LauncherSettings, homeChanges: Boolean, lockChanges: Boolean) {
    PreviewStage {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val each: Dp = min((maxWidth - 12.dp) / 2, 124.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                LabelledPreview("Home screen", homeChanges) { HomePreview(home, each, border = Color.White.copy(alpha = 0.5f)) }
                LabelledPreview("Lock screen", lockChanges) { LockPreview(lock, each, border = Color.White.copy(alpha = 0.5f)) }
            }
        }
    }
}

@Composable
private fun TargetPicker(target: ApplyTarget, onChange: (ApplyTarget) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StudioLabel("Apply to")
        StudioSegmented(ApplyTarget.entries, target, { it.label }, onChange, tag = { TestTags.applyTarget(it.name) })
    }
}

/** Theme sheet: previews, where to apply, and what else to change on the phone. */
@Composable
private fun ThemeApplySheet(
    theme: ThemeBundle,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (ThemeApplyRequest) -> Unit,
) {
    var target by rememberSaveable(theme.id) { mutableStateOf(ApplyTarget.BOTH) }
    var useThemeWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setPhoneWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var showLockScreen by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setLockWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }

    // The user has a wallpaper of their own (photo, or one picked in Wallpapers) that this theme would replace.
    val ownWallpaper = settings.wallpaperUri != null && settings.wallpaperUri != theme.wallpaperUri
    val replace = !ownWallpaper || useThemeWallpaper
    val themedHome = settings.withTheme(theme, useThemeWallpaper = replace)
    val homeAfter = if (target.home) themedHome else settings
    val lockAfter = when (target) {
        // Home only: a lock screen that follows home changes too; one with its own theme does not.
        ApplyTarget.HOME -> if (settings.lockThemeId == null && settings.lockWallpaperUri == null) themedHome else settings.lockLook()
        ApplyTarget.LOCK -> settings.withTheme(theme, useThemeWallpaper = true)
        ApplyTarget.BOTH -> themedHome
    }.copy(lockThemeId = null, lockWallpaperUri = null)
    val lockChanges = target.lock || (target.home && settings.lockThemeId == null)

    SheetScaffold(
        label = theme.category.label, title = theme.name, subtitle = theme.tagline, tag = TestTags.APPLY_SHEET,
        onDismiss = onDismiss, confirm = "Apply theme",
        onConfirm = {
            onApply(
                ThemeApplyRequest(
                    theme = theme,
                    target = target,
                    replaceWallpaper = target.home && replace,
                    setPhoneWallpaper = target.home && setPhoneWallpaper,
                    showLockScreen = target.lock && showLockScreen,
                    setLockWallpaper = target.lock && setLockWallpaper,
                ),
            )
        },
    ) {
        SheetPreviews(homeAfter.copy(lockThemeId = null, lockWallpaperUri = null), lockAfter, target.home, lockChanges)
        TargetPicker(target) { target = it }
        Column {
            if (target.home && ownWallpaper) {
                StudioCheckRow(
                    if (theme.wallpaperId != null) "Use the theme's wallpaper instead of mine" else "Use the theme's background instead of my wallpaper",
                    null, useThemeWallpaper, TestTags.OPT_THEME_BACKGROUND,
                ) { useThemeWallpaper = it }
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

/** Recessed well that previews sit in, so phone frames read as objects on a stage. */
@Composable
private fun PreviewStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Studio.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(if (c.dark) Color.Black.copy(alpha = 0.25f) else Color(0x0F111827), StudioCardCorner)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun LabelledPreview(label: String, changed: Boolean, content: @Composable () -> Unit) {
    val c = Studio.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.shadow(10.dp, RoundedCornerShape(6.dp), clip = false, ambientColor = c.shadow, spotColor = c.shadow)) { content() }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            StudioText(label, StudioType.caption.copy(fontWeight = FontWeight.Medium), if (changed) c.ink else c.muted, maxLines = 1)
            if (!changed) StudioText("· no change", StudioType.caption, c.muted, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- Widgets

@Composable
private fun WidgetsPage(actions: CustomiseActions, widgetStatus: String?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).readableWidth().padding(horizontal = Gutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StudioHint("Clock widgets for any home screen that supports widgets. They keep time on their own, so they never wake your phone.", Modifier.padding(horizontal = 4.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardW = if (maxWidth >= 600.dp) (maxWidth - 12.dp) / 2 else maxWidth
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ClockWidgetKind.entries.forEach { kind -> WidgetCard(kind, Modifier.width(cardW)) { actions.onAddWidget(kind) } }
            }
        }
        widgetStatus?.let { StudioHint(it, Modifier.padding(horizontal = 4.dp)) }
        StudioHint("Using Versity as your home app? Its home screen shows the clock you pick in Clock and style. Widgets placed directly on Versity's home screen are coming next.", Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun WidgetCard(kind: ClockWidgetKind, modifier: Modifier, onAdd: () -> Unit) {
    val serif = remember { fontFamilyFor(ThemePresets.font("cormorant")) }
    StudioCard(modifier) {
        Box(
            Modifier.fillMaxWidth().height(136.dp).clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.wp_leviathan_thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("11:11", color = Color(0xFFDCEBFA), fontFamily = serif, fontSize = 46.sp, fontWeight = FontWeight.Light, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "lnum"))
                Canvas(Modifier.size(width = 150.dp, height = 12.dp)) {
                    val y = size.height / 2
                    val cx = size.width / 2
                    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xBFDCEBFA)), 0f, cx - 12f), Offset(0f, y), Offset(cx - 12f, y), 1.dp.toPx())
                    drawLine(Brush.horizontalGradient(listOf(Color(0xBFDCEBFA), Color.Transparent), cx + 12f, size.width), Offset(cx + 12f, y), Offset(size.width, y), 1.dp.toPx())
                    val r = size.height * 0.45f
                    val k = r * 0.18f
                    drawPath(Path().apply {
                        moveTo(cx, y - r); quadraticTo(cx + k, y - k, cx + r, y); quadraticTo(cx + k, y + k, cx, y + r)
                        quadraticTo(cx - k, y + k, cx - r, y); quadraticTo(cx - k, y - k, cx, y - r); close()
                    }, Color(0xFFDCEBFA))
                }
                if (kind.showsDate) {
                    Text("WEDNESDAY  ·  11 NOVEMBER", color = Color(0xFFB3CBE3), fontFamily = serif, fontSize = 10.sp, letterSpacing = 1.6.sp, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "lnum"))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StudioText(kind.title, StudioType.cardTitle)
            StudioText(kind.description, StudioType.small, Studio.colors.muted)
        }
        StudioPrimaryButton("Add to home screen", Modifier.testTag(TestTags.addWidget(kind.name)), onAdd)
    }
}

// ---------------------------------------------------------------- More

@Composable
private fun MorePage(settings: LauncherSettings, actions: CustomiseActions, go: (CustomisePage) -> Unit, onReset: () -> Unit) {
    val c = Studio.colors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).readableWidth().padding(horizontal = Gutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.fillMaxWidth().glass(c).padding(vertical = 4.dp)) {
            listOf(
                Triple(CustomisePage.STYLE, "Clock, layout, colours and font", Color(0xFF8C7BF5) to Color(0xFF5845D8)),
                Triple(CustomisePage.LOCK, lockThemeLabel(settings), Color(0xFF6FA8F5) to Color(0xFF3D6FD8)),
                Triple(CustomisePage.DRAWER, if (settings.drawerStyle == DrawerStyle.PLAIN) "Plain drawer, original icons" else "Themed drawer", Color(0xFF6CD3A8) to Color(0xFF2E9E78)),
            ).forEachIndexed { i, (page, detail, tint) ->
                if (i > 0) StudioDivider(Modifier.padding(start = 64.dp))
                ListRow(page.title, detail, tint, Modifier.testTag(TestTags.nav(page.name))) { go(page) }
            }
        }
        StudioCard(title = "App appearance") {
            StudioSegmented(UiMode.entries, settings.uiMode, ::uiModeLabel, { actions.onUiMode(it) }, tag = { TestTags.uiMode(it.name) })
            StudioHint("Light or dark for these screens and the plain app drawer. System follows your phone.")
        }
        StudioCard(title = "About") {
            StudioHint("Fonts: Inter, Space Grotesk, Outfit, Roboto Mono, Cormorant Garamond, Playfair Display, Josefin Sans and Syne, under the SIL Open Font License 1.1. Licence texts are in the app's assets/licenses folder.")
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StudioTextButton("Reset everything", Modifier.testTag(TestTags.RESET), color = Color(0xFFE5484D), onClick = onReset)
        }
    }
}

@Composable
private fun ListRow(title: String, detail: String, tint: Pair<Color, Color>, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ClayTile(tint.first, tint.second, 34.dp) { SparkleGlyph(Color.White, 13.dp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            StudioText(title, StudioType.bodyStrong, maxLines = 1)
            StudioText(detail, StudioType.caption, c.muted, maxLines = 1)
        }
        Canvas(Modifier.size(12.dp)) {
            val w = size.width
            drawLine(c.muted, Offset(w * 0.35f, w * 0.15f), Offset(w * 0.7f, w * 0.5f), 1.6.dp.toPx(), StrokeCap.Round)
            drawLine(c.muted, Offset(w * 0.7f, w * 0.5f), Offset(w * 0.35f, w * 0.85f), 1.6.dp.toPx(), StrokeCap.Round)
        }
    }
}

// ---------------------------------------------------------------- Clock and style

@Composable
private fun StylePage(settings: LauncherSettings, actions: CustomiseActions, previewing: Boolean, onEditClock: () -> Unit, onPreviewing: (Boolean) -> Unit) {
    // Sliders hold the finger position locally and save on release, so dragging never waits on disk.
    var dragDim by remember { mutableStateOf<Int?>(null) }
    var dragAccent by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(settings.dimPercent) { dragDim = null }
    LaunchedEffect(settings.accentArgb) { dragAccent = null }
    val live = settings.copy(dimPercent = dragDim ?: settings.dimPercent, accentArgb = dragAccent ?: settings.accentArgb)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).readableWidth().padding(horizontal = Gutter, vertical = 8.dp)) {
            StyleControls(settings, actions, live, { dragDim = it }, { dragAccent = it }, onEditClock)
            Spacer(Modifier.height(24.dp))
        }
        // Preview (in the header) shows the real result full size; the page itself stays a calm list.
        AnimatedVisibility(visible = previewing, enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(StudioMotion.QUICK))) {
            BackHandler { onPreviewing(false) }
            val c = Studio.colors
            BoxWithConstraints(
                Modifier.fillMaxSize().background(c.scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPreviewing(false) },
                contentAlignment = Alignment.Center,
            ) {
                val each = min((maxWidth - 48.dp) / 2, (maxHeight - 96.dp) * (360f / 780f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Previews(live, each)
                    StudioText("Tap anywhere to close", StudioType.caption, Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

/** Home and lock side by side, [each] wide. Tagged so tests can read which palette is previewed. */
@Composable
private fun Previews(settings: LauncherSettings, each: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HomePreview(settings, each, border = Color.White.copy(alpha = 0.5f))
            StudioText("Home screen", StudioType.caption, Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LockPreview(settings, each, border = Color.White.copy(alpha = 0.5f))
            StudioText("Lock screen", StudioType.caption, Color.White)
        }
    }
}

@Composable
private fun StyleControls(
    settings: LauncherSettings,
    actions: CustomiseActions,
    live: LauncherSettings,
    onDimDrag: (Int?) -> Unit,
    onAccentDrag: (Long?) -> Unit,
    onEditClock: () -> Unit,
) {
    val c = Studio.colors
    // Hidden semantics anchor so tests can read the live palette without opening the preview.
    Box(Modifier.size(1.dp).testTag(TestTags.PREVIEW).semantics { previewPalette = live.paletteId })
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioCard(title = "Clock") {
            StudioLabel("Classic")
            ChoiceRow {
                ThemePresets.clockStyles.filterNot { it in BigClockStyles }.forEach { style ->
                    StudioChip(clockStyleLabel(style), settings.clockStyle == style, TestTags.clockStyle(style.name)) { actions.onClockStyle(style) }
                }
            }
            StudioLabel("Big clock")
            ChoiceRow {
                ThemePresets.clockStyles.filter { it in BigClockStyles }.forEach { style ->
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
        StudioCard(title = "Position and size") {
            val home = settings.clockPlacement
            val lock = settings.lockClockPlacement
            fun describe(p: ClockPlacement?) = p?.let { "custom, ${(it.scale * 100).roundToInt()}%" } ?: "layout default"
            StudioHint("Home: ${describe(home)} · Lock: ${describe(lock)}")
            StudioButton("Move and resize clock", Modifier.testTag(TestTags.EDIT_CLOCK)) { onEditClock() }
        }
        StudioCard(title = "Layout") {
            StudioSegmented(HomeLayout.entries, settings.layout, ::layoutLabel, { actions.onLayout(it) }, tag = { "layout_${it.name}" })
        }
        StudioCard(title = "Background") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioText("Dim", StudioType.body, modifier = Modifier.width(48.dp))
                ThinSlider(
                    value = live.dimPercent.toFloat(),
                    onChange = { onDimDrag(it.toInt()) },
                    onFinished = { actions.onDim(live.dimPercent) },
                    range = LauncherSettings.MIN_DIM.toFloat()..LauncherSettings.MAX_DIM.toFloat(),
                    modifier = Modifier.weight(1f).testTag(TestTags.DIM_SLIDER),
                    description = "Dim, ${live.dimPercent} percent",
                )
                StudioText("${live.dimPercent}%", StudioType.small, c.muted, Modifier.width(44.dp).padding(start = 8.dp))
            }
            StudioDivider()
            StudioLabel("Backdrop")
            StudioHint("Drawn over the colours when there is no wallpaper.")
            ChoiceRow {
                Backdrop.entries.forEach { b ->
                    StudioChip(backdropLabel(b), settings.backdrop == b, "backdrop_option_${b.name}") { actions.onBackdrop(b) }
                }
            }
        }
        StudioCard(title = "Colours") {
            ChoiceRow {
                ThemePresets.palettes.forEach { p ->
                    PaletteTile(p.name, p.backgroundTop, p.backgroundBottom, p.accent, settings.paletteId == p.id, TestTags.palette(p.id)) { actions.onPalette(p.id) }
                }
            }
            StudioDivider()
            StudioLabel("Accent")
            ChoiceRow {
                StudioChip("From palette", settings.accentArgb == null, TestTags.accent(null)) { actions.onAccent(null) }
                AccentSwatches.forEach { argb -> Swatch(argb, settings.accentArgb == argb, TestTags.accent(argb)) { actions.onAccent(argb) } }
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
    }
}

// ---------------------------------------------------------------- Lock screen

@Composable
private fun LockPage(
    settings: LauncherSettings,
    actions: CustomiseActions,
    lockWallpaperStatus: String?,
    onBrowseThemes: () -> Unit,
    onRequestLockWallpaper: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val previewW = min(maxWidth * 0.38f, 136.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).readableWidth().padding(horizontal = Gutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewStage { LabelledPreview("Lock screen", true) { LockPreview(settings, previewW, border = Color.White.copy(alpha = 0.5f)) } }
            StudioCard(title = "Look") {
                StudioText(lockThemeLabel(settings), StudioType.body)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StudioButton("Browse themes", Modifier.testTag(TestTags.BROWSE_THEMES)) { onBrowseThemes() }
                    if (settings.lockThemeId != null || settings.lockWallpaperUri != null) {
                        StudioTextButton("Match home screen", Modifier.testTag(TestTags.LOCK_FOLLOW_HOME), color = Studio.colors.brandText) { actions.onLockFollowsHome() }
                    }
                }
                StudioHint("Open a theme or wallpaper and choose Lock to give the lock screen its own look.")
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
}

// ---------------------------------------------------------------- Drawer and icons

@Composable
private fun DrawerPage(settings: LauncherSettings, actions: CustomiseActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).readableWidth().padding(horizontal = Gutter, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StudioCard(title = "App drawer") {
            StudioSegmented(
                DrawerStyle.entries, settings.drawerStyle,
                { if (it == DrawerStyle.PLAIN) "Plain" else "Match theme" }, { actions.onDrawerStyle(it) },
                tag = { TestTags.drawerStyle(it.name) },
            )
            StudioHint("Plain stays neutral and follows App appearance. Themes never restyle it unless you choose Match theme.")
        }
        StudioCard(title = "App icons") {
            StudioSegmented(
                IconStyle.entries, settings.iconStyle,
                { if (it == IconStyle.ORIGINAL) "Original" else "Tint to theme" }, { actions.onIconStyle(it) },
                tag = { TestTags.iconStyle(it.name) },
            )
            StudioHint("Original keeps every app's own icon. Tint uses the app's themed icon where it has one.")
        }
    }
}

// ---------------------------------------------------------------- Shared pieces

@Composable
private fun ChoiceRow(content: @Composable () -> Unit) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }

/** Confirmation in the studio style: a question, the consequence, and buttons that name the action. */
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
            Modifier.fillMaxWidth()
                .shadow(24.dp, StudioCardCorner, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                .background(c.surface, StudioCardCorner)
                .border(1.dp, Brush.verticalGradient(listOf(c.glassRimTop, c.line)), StudioCardCorner)
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
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
            .clip(StudioChipCorner)
            .background(if (selected) c.brandSoft else c.glass, StudioChipCorner)
            .border(1.dp, if (selected) c.brand else c.glassRimTop, StudioChipCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(5.dp)
            .semantics { contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(7.dp)).background(Brush.verticalGradient(listOf(Color(top), Color(bottom))))) {
            Box(Modifier.align(Alignment.BottomStart).padding(5.dp).size(width = 14.dp, height = 3.dp).background(Color(accent), RoundedCornerShape(2.dp)))
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
            .size(38.dp)
            .clip(StudioChipCorner)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.ink else c.glassRimTop, StudioChipCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(5.dp)
            .background(Color(argb), RoundedCornerShape(6.dp))
            .semantics { contentDescription = "Accent colour" },
    )
}

@Composable
private fun HueSlider(settings: LauncherSettings, actions: CustomiseActions, onDrag: (Long?) -> Unit) {
    val current = settings.accentArgb ?: ThemePresets.palette(settings.paletteId).accent
    val hue = FloatArray(3).also { AndroidColor.colorToHSV(current.toInt(), it) }[0]
    val spectrum = remember { Brush.horizontalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 0.6f, 1f))) }) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StudioText("Hue", StudioType.body, modifier = Modifier.width(48.dp))
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

/** Slim slider: rounded 4dp track, small clay thumb, 48dp touch height. */
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
                Modifier.size(20.dp)
                    .shadow(4.dp, RoundedCornerShape(10.dp), clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.5.dp, c.brand, RoundedCornerShape(10.dp)),
            )
        },
        track = { state ->
            val frac = (state.value - range.start) / (range.endInclusive - range.start)
            if (trackBrush != null) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(trackBrush))
            } else {
                Box(Modifier.fillMaxWidth().height(4.dp).background(c.line, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(c.brand, RoundedCornerShape(2.dp)))
                }
            }
        },
    )
}


// ---------------------------------------------------------------- Clock position editor

private val SnapLinesY = listOf(0.2f, 1f / 3f, 0.5f)

/**
 * Full-screen editor over the real home or lock screen: drag anywhere to move the clock, pinch or use
 * the slider to resize. It snaps to the centre line and to thirds, with a guide and a light tick.
 */
@Composable
private fun ClockEditor(
    settings: LauncherSettings,
    onDone: (ClockPlacement?, ClockPlacement?) -> Unit,
    onCancel: () -> Unit,
) {
    val c = Studio.colors
    val haptics = LocalHapticFeedback.current
    var target by rememberSaveable { mutableStateOf(ApplyTarget.HOME) }
    var home by remember { mutableStateOf(settings.clockPlacement) }
    var lock by remember { mutableStateOf(settings.lockClockPlacement) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf<Float?>(null) }
    val current = if (target == ApplyTarget.HOME) home else lock
    fun update(p: ClockPlacement?) { if (target == ApplyTarget.HOME) home = p else lock = p }
    BackHandler(onBack = onCancel)

    Box(Modifier.fillMaxSize().testTag(TestTags.CLOCK_EDITOR).background(Color.Black)) {
        val now = remember { java.time.LocalDateTime.now() }
        if (target == ApplyTarget.HOME) {
            HomeContent(HomeUiState(settings = settings.copy(clockPlacement = home ?: ClockPlacement.DEFAULT), batteryPercent = 72, now = now, isDefaultLauncher = true))
        } else {
            LockStyleContent(HomeUiState(settings = settings.lockLook().copy(clockPlacement = lock ?: ClockPlacement.DEFAULT), batteryPercent = 72, now = now, isDefaultLauncher = true), onUnlock = {})
        }
        // Gesture layer: owns every touch, so the screen underneath never reacts.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .pointerInput(target) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var raw = (if (target == ApplyTarget.HOME) home else lock) ?: ClockPlacement.DEFAULT
                        do {
                            val event = awaitPointerEvent()
                            val pan = event.calculatePan()
                            val zoom = event.calculateZoom()
                            raw = ClockPlacement(raw.x + pan.x / size.width, raw.y + pan.y / size.height, raw.scale * zoom).clamped()
                            val sx = abs(raw.x - 0.5f) < 0.025f
                            val sy = SnapLinesY.firstOrNull { abs(raw.y - it) < 0.018f }
                            if ((sx && !snapX) || (sy != null && sy != snapY)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            snapX = sx
                            snapY = sy
                            update(raw.copy(x = if (sx) 0.5f else raw.x, y = sy ?: raw.y))
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        snapX = false
                        snapY = null
                    }
                },
        ) {
            val guide = c.brandLight
            if (snapX) Box(Modifier.align(Alignment.TopCenter).width(1.dp).fillMaxHeight().background(guide))
            snapY?.let { y -> Box(Modifier.offset(y = maxHeight * y).fillMaxWidth().height(1.dp).background(guide)) }
        }
        // Top bar
        Row(
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth().editorPanel(c).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioTextButton("Cancel", onClick = onCancel)
            Box(Modifier.weight(1f)) {
                StudioSegmented(listOf(ApplyTarget.HOME, ApplyTarget.LOCK), target, { if (it == ApplyTarget.HOME) "Home" else "Lock" }, { target = it })
            }
            StudioPrimaryButton("Done", Modifier.testTag(TestTags.EDITOR_DONE).padding(start = 4.dp)) { onDone(home, lock) }
        }
        // Bottom panel
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.systemBars).padding(12.dp)
                .widthIn(max = 560.dp).fillMaxWidth().editorPanel(c).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StudioHint("Drag anywhere to move the clock. Pinch or use the slider to resize.")
            val p = current ?: ClockPlacement.DEFAULT
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioText("Size", StudioType.body, modifier = Modifier.width(44.dp))
                ThinSlider(
                    value = p.scale,
                    onChange = { update(p.copy(scale = it).clamped()) },
                    range = ClockPlacement.MIN_SCALE..ClockPlacement.MAX_SCALE,
                    modifier = Modifier.weight(1f),
                    description = "Clock size, ${(p.scale * 100).roundToInt()} percent",
                )
                StudioText("${(p.scale * 100).roundToInt()}%", StudioType.small, c.muted, Modifier.width(48.dp).padding(start = 8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudioButton("Centre") { update(p.copy(x = 0.5f)) }
                StudioTextButton("Use layout position", Modifier.testTag(TestTags.EDITOR_RESET), color = c.brandText) { update(null) }
            }
        }
    }
}

/** Editor bars sit over a live screen, so they are near-opaque rather than glass, keeping controls legible. */
private fun Modifier.editorPanel(c: com.versity.launcher.ui.studio.StudioColors): Modifier =
    this.shadow(16.dp, StudioCardCorner, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
        .background(c.surface.copy(alpha = 0.95f), StudioCardCorner)
        .border(1.dp, Brush.verticalGradient(listOf(c.glassRimTop, c.line)), StudioCardCorner)
