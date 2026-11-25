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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.versity.launcher.system.Release
import com.versity.launcher.system.UpdateState
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
    /** Takes a photo out of the user's photos (and deletes Versity's copy). */
    fun onRemovePhoto(uri: String) {}
    fun onAutoUpdateCheck(enabled: Boolean) {}
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
    fun onCheckUpdate() {}
    fun onDownloadUpdate(release: Release) {}
    fun onInstallUpdate(ready: UpdateState.Ready) {}
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
    /** A photo the user just picked: open its sheet so they can choose where it goes. */
    data class Photo(val uri: String, override val returnWhenDone: Boolean = false) : CustomiseRequest
}

/** A wallpaper the sheet can apply: one that ships with Versity, or one of the user's photos. */
data class WallpaperChoice(
    val uri: String,
    val name: String,
    val collection: String,
    /** Light image: text on it needs dark ink. */
    val light: Boolean,
    val isPhoto: Boolean,
) {
    companion object {
        fun of(wp: WallpaperOption) = WallpaperChoice(wp.uri, wp.name, wp.collection, wp.light, isPhoto = false)
        fun photo(uri: String, light: Boolean = false) = WallpaperChoice(uri, "Your photo", "Your photos", light, isPhoto = true)
    }
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
    val wallpaper: WallpaperChoice,
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
    update: UpdateState = UpdateState.Idle,
) = StudioTheme(settings.uiMode) {
    val c = Studio.colors
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var forward by remember { mutableStateOf(true) }
    var sheetThemeId by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetWallpaperUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var arranging by remember { mutableStateOf(false) }
    var studio by remember { mutableStateOf<StudioTarget?>(null) }
    var returnWhenDone by remember { mutableStateOf(false) }
    var studioMessage by remember { mutableStateOf<String?>(null) }
    // Tablets get a sidebar instead of the hub, and Settings side by side with its pages when there is room.
    val config = LocalConfiguration.current
    val tablet = config.screenWidthDp >= 720 && config.screenHeightDp >= 480
    val sidebar = sidebarWidth(config.screenWidthDp.dp)
    val twoPane = tablet && config.screenWidthDp.dp - sidebar >= 720.dp
    val go: (CustomisePage) -> Unit = { forward = true; page = it }
    val up: () -> Unit = {
        val parent = page.parent
        when {
            // Beside the sidebar, a top-level section has nowhere to go back to but out.
            tablet && (parent == null || parent == CustomisePage.HUB) -> onBack()
            tablet && twoPane && parent == CustomisePage.SETTINGS -> onBack()
            parent != null -> { forward = false; page = parent }
            else -> onBack()
        }
    }
    BackHandler(enabled = if (tablet) page.parent == CustomisePage.SETTINGS && !twoPane else page != CustomisePage.HUB) { up() }
    val toggleMode = { actions.onUiMode(if (c.dark) UiMode.LIGHT else UiMode.DARK) }
    val updateActions = UpdateActions(actions::onCheckUpdate, actions::onDownloadUpdate, actions::onInstallUpdate)
    val previewAction: (CustomisePage) -> (@Composable () -> Unit)? = { current ->
        if (current == CustomisePage.CLOCK || current == CustomisePage.COLOURS) {
            { StudioTextButton("Preview", Modifier.testTag(TestTags.PREVIEW_FAB), color = c.brandText) { previewing = true } }
        } else null
    }

    /** The body of one page, the same on phones and tablets. */
    @Composable
    fun Body(current: CustomisePage, selectedDetail: CustomisePage? = null) {
        when (current) {
            CustomisePage.HUB -> HubPage(settings, isDefaultLauncher, go, actions::onSetDefaultLauncher, update, updateActions)
            CustomisePage.THEMES -> ThemesPage(settings, isDefaultLauncher) { sheetThemeId = it.id }
            CustomisePage.WALLPAPERS -> WallpapersPage(settings, actions, isDefaultLauncher) { sheetWallpaperUri = it }
            CustomisePage.WIDGETS -> WidgetsPage(
                settings, widgetStatus,
                onCreate = { studioMessage = null; studio = StudioTarget.New(it) },
                onEdit = { studioMessage = null; studio = it },
                onArrange = { arranging = true },
            )
            CustomisePage.SETTINGS -> SettingsPage(
                settings, actions, isDefaultLauncher, go, onReset = { showResetDialog = true },
                update = update, updateActions = updateActions, versionName = versionName, selected = selectedDetail,
            )
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

    LaunchedEffect(request) {
        when (request) {
            is CustomiseRequest.Studio -> { studio = request.target; studioMessage = null }
            is CustomiseRequest.Arrange -> arranging = true
            is CustomiseRequest.Photo -> { forward = true; page = CustomisePage.WALLPAPERS; sheetWallpaperUri = request.uri }
            null -> return@LaunchedEffect
        }
        returnWhenDone = request.returnWhenDone
        onRequestHandled()
    }
    fun finishOverlay() {
        if (returnWhenDone) { returnWhenDone = false; onBack() }
    }

    Box(modifier.fillMaxSize().background(c.page)) {
        if (tablet) {
            val section = sectionOf(page)
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Sidebar(
                    sidebar, section, settings, isDefaultLauncher, update, updateActions,
                    onSelect = { forward = true; page = if (it == CustomisePage.SETTINGS && twoPane) CustomisePage.CLOCK else it },
                    onSetDefault = actions::onSetDefaultLauncher,
                    dark = c.dark, onToggleTheme = toggleMode,
                )
                PaneDivider()
                if (section == CustomisePage.SETTINGS && twoPane) {
                    val detail = page.takeIf { it.parent == CustomisePage.SETTINGS } ?: CustomisePage.CLOCK
                    Column(Modifier.width(380.dp).fillMaxHeight()) {
                        PaneHeader("Settings", onBack = null)
                        Body(CustomisePage.SETTINGS, selectedDetail = detail)
                    }
                    PaneDivider()
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(targetState = detail, transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(120))) }, label = "detail") { d ->
                            Column(Modifier.fillMaxSize()) {
                                PaneHeader(d.title, onBack = null, action = previewAction(d))
                                Body(d)
                            }
                        }
                    }
                } else {
                    val shown = if (page == CustomisePage.HUB) CustomisePage.THEMES else page
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(targetState = shown, transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(120))) }, label = "pane") { current ->
                            Column(Modifier.fillMaxSize()) {
                                val inner = current.parent == CustomisePage.SETTINGS
                                PaneHeader(current.title, onBack = if (inner) up else null, action = previewAction(current))
                                Body(current)
                            }
                        }
                    }
                }
            }
        } else {
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
                            PageHeader(current.title, c.dark, onBack = up, action = previewAction(current), onToggleTheme = toggleMode)
                        }
                        Body(current)
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
            // A widget on the phone's home screen can be removed while it is being edited. Then there is
            // nothing left to save to, so the studio offers to add it again instead.
            val changes = com.versity.launcher.widget.Widgets.changes.intValue
            var gone by remember(target) { mutableStateOf(false) }
            if (target is StudioTarget.System) {
                LaunchedEffect(target, changes) { gone = !com.versity.launcher.widget.Widgets.isPlaced(context, target.id) }
                androidx.lifecycle.compose.LifecycleResumeEffect(target) {
                    gone = !com.versity.launcher.widget.Widgets.isPlaced(context, target.id)
                    onPauseOrDispose { }
                }
            }
            val adding = target is StudioTarget.New || gone
            WidgetStudio(
                initial = initial,
                title = if (adding) "New widget" else "Edit widget",
                confirmLabel = if (adding) "Add to home screen" else "Save",
                compactConfirmLabel = if (adding) "Add" else null,
                wallpaperUri = settings.wallpaperUri,
                message = studioMessage ?: if (gone) "This widget is no longer on your home screen. Add it again with this look, or close." else null,
                destructive = (target as? StudioTarget.Home)?.let { home ->
                    "Remove from home screen" to { actions.onRemoveHomeWidget(home.id); studio = null; finishOverlay() }
                },
                onConfirm = { spec ->
                    if (adding) {
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
            ThemeApplySheet(theme, settings, isDefaultLauncher, onDismiss = { sheetThemeId = null }) { request ->
                sheetThemeId = null
                actions.onApplyTheme(request)
            }
        }
        val sheetWallpaper = sheetWallpaperUri?.let { uri ->
            Wallpapers.byUri(uri)?.let(WallpaperChoice::of) ?: uri.takeIf { it in settings.photos }?.let { WallpaperChoice.photo(it) }
        }
        SheetHost(visible = sheetWallpaper != null, onDismiss = { sheetWallpaperUri = null }, key = sheetWallpaper) { wp ->
            WallpaperApplySheet(
                wp, settings, isDefaultLauncher,
                onDismiss = { sheetWallpaperUri = null },
                onRemovePhoto = { sheetWallpaperUri = null; actions.onRemovePhoto(it) },
            ) { request ->
                sheetWallpaperUri = null
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
            body = "This clears the theme you applied, your photos, dock apps, widgets on Versity's home screen, lock screen and drawer choices. Your phone's own wallpapers and widgets on other home screens aren't touched.",
            cancel = "Keep settings", confirm = "Reset",
            cancelTag = TestTags.RESET_CANCEL, confirmTag = TestTags.RESET_CONFIRM,
            onConfirm = { showResetDialog = false; actions.onReset() },
            onCancel = { showResetDialog = false },
        )
    }
}
