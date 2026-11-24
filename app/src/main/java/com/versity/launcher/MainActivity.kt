package com.versity.launcher

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.system.LockWallpaper
import androidx.compose.runtime.remember
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.system.AppLauncher
import com.versity.launcher.system.DefaultLauncherCheck
import com.versity.launcher.system.batteryPercentFlow
import com.versity.launcher.system.minuteTicker
import com.versity.launcher.ui.drawer.AppDrawerContent
import com.versity.launcher.ui.drawer.DrawerViewModel
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeViewModel
import com.versity.launcher.ui.home.persistWallpaper
import com.versity.launcher.ui.lock.LockStyleContent
import com.versity.launcher.ui.settings.CustomiseActions
import com.versity.launcher.ui.settings.CustomiseContent
import com.versity.launcher.ui.settings.CustomiseViewModel
import com.versity.launcher.ui.theme.DrawerTheme
import com.versity.launcher.ui.theme.plainDrawerIsLight
import com.versity.launcher.ui.studio.studioIsDark
import com.versity.launcher.ui.settings.ApplyTarget
import com.versity.launcher.ui.settings.ThemeApplyRequest
import com.versity.launcher.domain.lockLook
import com.versity.launcher.domain.withTheme
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.IconStyle
import com.versity.launcher.data.Ornament
import com.versity.launcher.widget.ClockWidgetKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen { HOME, DRAWER, CUSTOMISE }

class MainActivity : ComponentActivity() {
    /** Bumped when HOME is pressed while we are already running, so we return to the home screen. */
    private var homeSignal by mutableIntStateOf(0)

    /**
     * True while the phone's own lock (keyguard) is engaged. The themed lock screen follows it: it shows
     * while the phone is locked and disappears when the user unlocks with PIN, fingerprint or face.
     */
    private var systemLocked by mutableStateOf(false)

    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }

    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // Switch to the lock screen while the display is still off, so the home screen is never
                // what appears over the keyguard. SCREEN_ON corrects it if the phone did not actually lock.
                Intent.ACTION_SCREEN_OFF -> systemLocked = true
                Intent.ACTION_SCREEN_ON -> systemLocked = keyguard?.isKeyguardLocked == true
                Intent.ACTION_USER_PRESENT -> systemLocked = false
            }
        }
    }

    private val appLauncher by lazy { AppLauncher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)
        val container = (application as LauncherApplication).container
        systemLocked = keyguard?.isKeyguardLocked == true
        ContextCompat.registerReceiver(
            this, lockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            val context = LocalContext.current
            val homeVm: HomeViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    HomeViewModel(
                        settings = container.settingsRepository,
                        apps = container.appsRepository,
                        battery = batteryPercentFlow(applicationContext),
                        clock = minuteTicker(),
                        isDefaultLauncher = container.isDefaultLauncher,
                        is24Hour = { DateFormat.is24HourFormat(applicationContext) },
                    )
                }
            })
            val drawerVm: DrawerViewModel = viewModel(factory = viewModelFactory {
                initializer { DrawerViewModel(container.appsRepository, container.settingsRepository) }
            })
            val customiseVm: CustomiseViewModel = viewModel(factory = viewModelFactory {
                initializer { CustomiseViewModel(container.settingsRepository) }
            })
            // One source of truth for every screen, so the theme never flashes the defaults.
            val home by homeVm.state.collectAsStateWithLifecycle()

            var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
            // Plain remember, not saveable: the counters live on the activity and reset on rotation, so must these.
            var seenHome by remember { mutableIntStateOf(homeSignal) }
            if (seenHome != homeSignal) {
                seenHome = homeSignal
                screen = Screen.HOME
            }
            var previewingLock by remember { mutableStateOf(false) }
            var lockWallpaperStatus by remember { mutableStateOf<String?>(null) }
            var widgetStatus by remember { mutableStateOf<String?>(null) }
            // The themed lock screen follows the phone's real lock state; there is no separate in-app lock.
            val lockUi = home.loaded && home.settings.lockStyleEnabled && systemLocked
            // Only the lock screen may ever be drawn over the keyguard. Everything else stays behind the phone's lock.
            LaunchedEffect(lockUi) { showOverKeyguard(lockUi) }

            LifecycleResumeEffect(Unit) {
                homeVm.refreshApps()
                onPauseOrDispose { }
            }

            // Home swallows Back, the lock-style screen ignores it, other screens return to Home.
            BackHandler(enabled = true) { if (screen != Screen.HOME) screen = Screen.HOME }

            val requestHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                container.isDefaultLauncher.value = DefaultLauncherCheck.isDefault(context)
            }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    // The copy fallback can be slow for large photos, so keep it off the main thread.
                    lifecycleScope.launch {
                        val stored = withContext(Dispatchers.IO) { runCatching { persistWallpaper(context, uri) }.getOrNull() }
                        if (stored != null) customiseVm.setWallpaper(stored)
                    }
                }
            }

            val showLock = lockUi || (home.loaded && screen == Screen.HOME && previewingLock)
            // Light studio screens (Customise, plain drawer) need dark status and navigation bar icons.
            val studioLight = !studioIsDark(home.settings.uiMode)
            val lightBars = !showLock && (
                (screen == Screen.DRAWER && plainDrawerIsLight(home.settings)) || (screen == Screen.CUSTOMISE && studioLight)
            )
            LaunchedEffect(lightBars) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            when {
                showLock -> LockStyleContent(
                    home.copy(settings = home.settings.lockLook()),
                    onUnlock = { if (lockUi) requestUnlock() else previewingLock = false },
                )
                screen == Screen.HOME -> HomeContent(
                    state = home,
                    onOpenDrawer = { drawerVm.refresh(); screen = Screen.DRAWER },
                    onOpenCustomise = { screen = Screen.CUSTOMISE },
                    onOpenPhone = { startSafely(Intent(Intent.ACTION_DIAL)) },
                    onSetDefault = {
                        try { requestHome.launch(DefaultLauncherCheck.requestIntent(context)) }
                        catch (_: ActivityNotFoundException) { startSafely(Intent(Settings.ACTION_SETTINGS)) }
                    },
                    onDismissBanner = homeVm::dismissBanner,
                    onLaunchApp = { app -> appLauncher.launch(app) },
                    onRemoveFavourite = { app -> customiseVm.setFavourites(home.settings.favourites - app.key) },
                )
                screen == Screen.DRAWER -> {
                    val drawer by drawerVm.state.collectAsStateWithLifecycle()
                    DrawerTheme(home.settings) {
                        AppDrawerContent(
                            state = drawer,
                            onQueryChange = drawerVm::onQueryChange,
                            onLaunch = { app: AppEntry ->
                                appLauncher.launch(app)
                                screen = Screen.HOME
                                drawerVm.onQueryChange("")
                            },
                            onToggleFavourite = drawerVm::toggleFavourite,
                        settings = home.settings,
                        )
                    }
                }
                else -> {
                    val actions = object : CustomiseActions {
                        override fun onPickWallpaper() {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        override fun onClearWallpaper() { customiseVm.setWallpaper(null) }
                        override fun onDim(percent: Int) { customiseVm.setDim(percent) }
                        override fun onClockStyle(style: ClockStyle) { customiseVm.setClockStyle(style) }
                        override fun onFont(id: String) { customiseVm.setFont(id) }
                        override fun onPalette(id: String) { customiseVm.setPalette(id) }
                        override fun onAccent(argb: Long?) { customiseVm.setAccent(argb) }
                        override fun onApplyTheme(request: ThemeApplyRequest) {
                            applyTheme(request, home.settings, customiseVm) { lockWallpaperStatus = it }
                        }
                        override fun onLockFollowsHome() { customiseVm.setLockTheme(null) }
                        override fun onDrawerStyle(style: DrawerStyle) { customiseVm.setDrawerStyle(style) }
                        override fun onIconStyle(style: IconStyle) { customiseVm.setIconStyle(style) }
                        override fun onUiMode(mode: com.versity.launcher.data.UiMode) { customiseVm.setUiMode(mode) }
                        override fun onAddWidget(kind: ClockWidgetKind) { widgetStatus = pinWidget(kind) }
                        override fun onLayout(layout: HomeLayout) { customiseVm.setLayout(layout) }
                        override fun onLockStyle(enabled: Boolean) { customiseVm.setLockStyle(enabled) }
                        override fun onBackdrop(backdrop: Backdrop) { customiseVm.setBackdrop(backdrop) }
                        override fun onOrnament(ornament: Ornament) { customiseVm.setOrnament(ornament) }
                        override fun onOrnamentDate(show: Boolean) { customiseVm.setOrnamentDate(show) }
                        override fun onPreviewLock() { previewingLock = true; screen = Screen.HOME }
                        override fun onSetLockWallpaper() {
                            lockWallpaperStatus = "Applying\u2026"
                            lifecycleScope.launch {
                                val result = LockWallpaper.apply(context.applicationContext, home.settings.lockLook())
                                lockWallpaperStatus = if (result.isSuccess) "Applied. Lock your phone to see it."
                                else "Could not apply: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                            }
                        }
                        override fun onReset() { customiseVm.reset() }
                    }
                    CustomiseContent(home.settings, actions, onBack = { screen = Screen.HOME }, lockWallpaperStatus = lockWallpaperStatus, widgetStatus = widgetStatus)
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(lockReceiver) }
        super.onDestroy()
    }

    /** Lets this activity appear over the keyguard (true) or keeps it behind the phone's lock (false). */
    private fun showOverKeyguard(show: Boolean) {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(show)
        } else {
            @Suppress("DEPRECATION")
            if (show) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }

    /**
     * Unlock gesture on the themed lock screen. When the phone is locked, the system asks for PIN,
     * pattern, fingerprint or face as usual; the lock screen only goes away once that succeeds.
     */
    private fun requestUnlock() {
        val km = keyguard
        if (km != null && km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { systemLocked = false }
            })
        } else {
            systemLocked = false
        }
    }

    /** Applies a theme to home, lock or both, and optionally to the phone's own wallpapers. */
    private fun applyTheme(
        request: ThemeApplyRequest,
        before: com.versity.launcher.data.LauncherSettings,
        customiseVm: CustomiseViewModel,
        onWallpaperStatus: (String) -> Unit,
    ) {
        val theme = request.theme
        if (request.target.home) customiseVm.applyTheme(theme, clearWallpaper = request.useThemeBackground)
        when (request.target) {
            ApplyTarget.LOCK -> customiseVm.setLockTheme(theme.id)
            ApplyTarget.BOTH -> customiseVm.setLockTheme(null)
            ApplyTarget.HOME -> Unit
        }
        if (request.showLockScreen) customiseVm.setLockStyle(true)

        val homeLook = before.withTheme(theme).copy(wallpaperUri = if (request.useThemeBackground) null else before.wallpaperUri)
        val lockLook = if (request.target == ApplyTarget.LOCK) before.withTheme(theme).copy(wallpaperUri = null) else homeLook
        val where = when (request.target) {
            ApplyTarget.HOME -> "home screen"
            ApplyTarget.LOCK -> "lock screen"
            ApplyTarget.BOTH -> "home and lock screens"
        }
        Toast.makeText(this, "${theme.name} applied to the $where", Toast.LENGTH_SHORT).show()
        if (!request.setPhoneWallpaper && !request.setLockWallpaper) return
        onWallpaperStatus("Setting wallpaper\u2026")
        lifecycleScope.launch {
            val app = applicationContext
            val results = if (request.setPhoneWallpaper && request.setLockWallpaper && homeLook == lockLook) {
                listOf(LockWallpaper.apply(app, homeLook, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK))
            } else {
                listOfNotNull(
                    if (request.setPhoneWallpaper) LockWallpaper.apply(app, homeLook, WallpaperManager.FLAG_SYSTEM) else null,
                    if (request.setLockWallpaper) LockWallpaper.apply(app, lockLook, WallpaperManager.FLAG_LOCK) else null,
                )
            }
            val failure = results.firstOrNull { it.isFailure }?.exceptionOrNull()
            onWallpaperStatus(
                if (failure == null) "Wallpaper set. Some phones' own lock-screen themes can still cover it."
                else "Could not set wallpaper: ${failure.message ?: "unknown error"}",
            )
        }
    }

    /** Asks the current home app to place a widget. Returns a message to show when that is not possible. */
    private fun pinWidget(kind: ClockWidgetKind): String {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, kind.provider)
        val asked = manager.isRequestPinAppWidgetSupported && runCatching { manager.requestPinAppWidget(provider, null, null) }.getOrDefault(false)
        return when {
            asked -> "Your home screen will ask where to put the widget."
            DefaultLauncherCheck.isDefault(this) ->
                "Versity can't hold widgets yet. With another home app, long-press its home screen, choose Widgets, then find Versity."
            else -> "Your home screen app can't add widgets from here. Long-press an empty spot on it, choose Widgets, then find Versity."
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal++
    }

    override fun onResume() {
        super.onResume()
        systemLocked = keyguard?.isKeyguardLocked == true
        (application as LauncherApplication).container.isDefaultLauncher.value = DefaultLauncherCheck.isDefault(this)
    }

    private fun startSafely(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
    }
}
