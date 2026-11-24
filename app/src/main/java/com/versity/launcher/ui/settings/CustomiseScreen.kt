package com.versity.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.HomeWidget
import com.versity.launcher.data.IconStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.Ornament
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.WallpaperOption
import com.versity.launcher.domain.Wallpapers
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioMotion
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioTheme
import com.versity.launcher.ui.widgets.WidgetStudio
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStore
import com.versity.launcher.widget.WidgetType

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
    fun onOrnament(ornament: Ornament) {}
    fun onOrnamentDate(show: Boolean) {}
    fun onDrawerStyle(style: DrawerStyle) {}
    fun onIconStyle(style: IconStyle) {}
    fun onUiMode(mode: UiMode) {}
    /** The arrange editor's result: both clock placements and every home widget, in one write. */
    fun onArrange(home: ClockPlacement?, lock: ClockPlacement?, widgets: List<HomeWidget>) {}
    fun onApplyTheme(request: ThemeApplyRequest) {}
    fun onApplyWallpaper(request: WallpaperApplyRequest) {}
    fun onLockFollowsHome() {}
    /** Places a new widget (on Versity's home screen when it is the home app). Returns a message to show, if any. */
    fun onCreateWidget(spec: WidgetSpec): String? = null
    fun onSaveWidget(target: StudioTarget, spec: WidgetSpec) {}
    fun onRemoveHomeWidget(id: Int) {}
    fun onPreviewLock() {}
    fun onSetLockWallpaper() {}
    fun onSetDefaultLauncher() {}
    fun onReset() {}
}

/** Where Customise is. HUB is the landing page; the detail pages sit under SETTINGS. */
enum class CustomisePage(val title: String) {
    HUB("Versity"), THEMES("Themes"), WALLPAPERS("Wallpapers"), WIDGETS("Widgets"), SETTINGS("Settings"),
    CLOCK("Clock"), COLOURS("Colours and font"), LAYOUT("Layout and position"), LOCK("Lock screen"),
    DRAWER("App drawer and icons"), ABOUT("About");

    val parent: CustomisePage? get() = when (this) {
        HUB -> null
        CLOCK, COLOURS, LAYOUT, LOCK, DRAWER, ABOUT -> SETTINGS
        else -> HUB
    }
}

enum class ApplyTarget(val label: String) {
    HOME("Home"), LOCK("Lock"), BOTH("Both");

    val home: Boolean get() = this != LOCK
    val lock: Boolean get() = this != HOME
}

/** Which widget the studio is editing. */
sealed interface StudioTarget {
    data class New(val type: WidgetType) : StudioTarget
    /** A widget on the phone's home screen (any launcher), by app-widget id. */
    data class System(val id: Int, val type: WidgetType) : StudioTarget
    /** A widget on Versity's own home screen. */
    data class Home(val id: Int) : StudioTarget
}

/** Something another screen asks Customise to open straight away. */
sealed interface CustomiseRequest {
    /** Close Customise again when the user is done, returning to where they came from. */
    val returnWhenDone: Boolean

    data class Studio(val target: StudioTarget, override val returnWhenDone: Boolean = true) : CustomiseRequest
    data class Arrange(override val returnWhenDone: Boolean = true) : CustomiseRequest
}

/** Everything the theme sheet decided. Options that do not fit the target are false. */
data class ThemeApplyRequest(
    val theme: ThemeBundle,
    val target: ApplyTarget,
    /** Home: swap the current wallpaper for the theme's own (its wallpaper, or its gradient). */
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
    request: CustomiseRequest? = null,
    onRequestHandled: () -> Unit = {},
    versionName: String = "",
) = StudioTheme(settings.uiMode) {
    val c = Studio.colors
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var forward by remember { mutableStateOf(true) }
    var sheetThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetWallpaperId by rememberSaveable { mutableStateOf<String?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var arranging by remember { mutableStateOf(false) }
    var studio by remember { mutableStateOf<StudioTarget?>(null) }
    var returnWhenDone by remember { mutableStateOf(false) }
    var studioMessage by remember { mutableStateOf<String?>(null) }
    val go: (CustomisePage) -> Unit = { forward = true; page = it }
    val up: () -> Unit = { page.parent?.let { forward = false; page = it } ?: onBack() }
    BackHandler(enabled = page != CustomisePage.HUB) { up() }
    val toggleMode = { actions.onUiMode(if (c.dark) UiMode.LIGHT else UiMode.DARK) }

    LaunchedEffect(request) {
        when (request) {
            is CustomiseRequest.Studio -> { studio = request.target; studioMessage = null }
            is CustomiseRequest.Arrange -> arranging = true
            null -> return@LaunchedEffect
        }
        returnWhenDone = request.returnWhenDone
        onRequestHandled()
    }
    fun finishOverlay() {
        if (returnWhenDone) { returnWhenDone = false; onBack() }
    }

    Box(modifier.fillMaxSize().background(c.page)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(300, easing = StudioMotion.Signature)) { it / 8 * dir } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(200, easing = StudioMotion.Exit)) { -it / 12 * dir } + fadeOut(tween(150)))
                },
                label = "page",
            ) { current ->
                Column(Modifier.fillMaxSize()) {
                    if (current == CustomisePage.HUB) {
                        HubHeader(c.dark, toggleMode)
                    } else {
                        PageHeader(
                            current.title, c.dark, onBack = up,
                            action = if (current == CustomisePage.CLOCK || current == CustomisePage.COLOURS) {
                                { StudioTextButton("Preview", Modifier.testTag(TestTags.PREVIEW_FAB), color = c.brandText) { previewing = true } }
                            } else null,
                            onToggleTheme = toggleMode,
                        )
                    }
                    when (current) {
                        CustomisePage.HUB -> HubPage(settings, isDefaultLauncher, go, actions::onSetDefaultLauncher)
                        CustomisePage.THEMES -> ThemesPage(settings) { sheetThemeId = it.id }
                        CustomisePage.WALLPAPERS -> WallpapersPage(settings, actions) { sheetWallpaperId = it.id }
                        CustomisePage.WIDGETS -> WidgetsPage(
                            settings, widgetStatus,
                            onCreate = { studioMessage = null; studio = StudioTarget.New(it) },
                            onEdit = { studioMessage = null; studio = it },
                            onArrange = { arranging = true },
                        )
                        CustomisePage.SETTINGS -> SettingsPage(settings, actions, isDefaultLauncher, go, onReset = { showResetDialog = true })
                        CustomisePage.CLOCK -> ClockPage(settings, actions)
                        CustomisePage.COLOURS -> ColoursPage(settings, actions)
                        CustomisePage.LAYOUT -> LayoutPage(settings, actions) { arranging = true }
                        CustomisePage.LOCK -> LockPage(
                            settings, actions, lockWallpaperStatus,
                            onBrowseThemes = { go(CustomisePage.THEMES) },
                            onRequestLockWallpaper = { showLockDialog = true },
                        )
                        CustomisePage.DRAWER -> DrawerPage(settings, actions)
                        CustomisePage.ABOUT -> AboutPage(versionName)
                    }
                }
            }
        }
        FullPreview(settings, previewing) { previewing = false }
        AnimatedVisibility(visible = arranging, enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(StudioMotion.QUICK))) {
            ArrangeEditor(
                settings,
                onDone = { home, lock, widgets -> arranging = false; actions.onArrange(home, lock, widgets); finishOverlay() },
                onCancel = { arranging = false; finishOverlay() },
            )
        }
        AnimatedVisibility(visible = studio != null, enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(StudioMotion.QUICK))) {
            var shown by remember { mutableStateOf(studio) }
            if (studio != null) shown = studio
            val target = shown ?: return@AnimatedVisibility
            val store = remember { WidgetStore(context) }
            val initial = remember(target) {
                when (target) {
                    is StudioTarget.New -> WidgetSpec.default(target.type)
                    is StudioTarget.System -> store.system(target.id, target.type)
                    is StudioTarget.Home -> store.home(target.id) ?: WidgetSpec.default(WidgetType.DIGITAL)
                }
            }
            WidgetStudio(
                initial = initial,
                title = if (target is StudioTarget.New) "New widget" else "Edit widget",
                confirmLabel = if (target is StudioTarget.New) "Add to home screen" else "Save",
                compactConfirmLabel = if (target is StudioTarget.New) "Add" else null,
                wallpaperUri = settings.wallpaperUri,
                message = studioMessage,
                destructive = (target as? StudioTarget.Home)?.let { home ->
                    "Remove from home screen" to { actions.onRemoveHomeWidget(home.id); studio = null; finishOverlay() }
                },
                onConfirm = { spec ->
                    if (target is StudioTarget.New) {
                        val message = actions.onCreateWidget(spec)
                        if (message != null) studioMessage = message else { studio = null; finishOverlay() }
                    } else {
                        actions.onSaveWidget(target, spec)
                        studio = null
                        finishOverlay()
                    }
                },
                onClose = { studio = null; finishOverlay() },
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
            body = "This returns Versity to its original look and clears your photo, dock apps, widgets on Versity's home screen, lock screen and drawer choices. Your phone's own wallpapers and widgets on other home screens aren't touched.",
            cancel = "Keep settings", confirm = "Reset",
            cancelTag = TestTags.RESET_CANCEL, confirmTag = TestTags.RESET_CONFIRM,
            onConfirm = { showResetDialog = false; actions.onReset() },
            onCancel = { showResetDialog = false },
        )
    }
}
