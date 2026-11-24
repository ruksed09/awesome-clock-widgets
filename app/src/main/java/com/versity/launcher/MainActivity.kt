package com.versity.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
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
import com.versity.launcher.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen { HOME, DRAWER, CUSTOMISE }

class MainActivity : ComponentActivity() {
    /** Bumped when HOME is pressed while we are already running, so we return to the home screen. */
    private var homeSignal by mutableIntStateOf(0)

    /** Bumped when the screen turns off while we are in front, so the lock-style screen shows again. */
    private var lockSignal by mutableIntStateOf(0)

    private val appLauncher by lazy { AppLauncher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = dark, navigationBarStyle = dark)
        val container = (application as LauncherApplication).container

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
            // Starts unlocked: a process restart (ColorOS kills launchers often) must never raise the lock screen by itself.
            var locked by rememberSaveable { mutableStateOf(false) }
            var previewingLock by remember { mutableStateOf(false) }
            var lockWallpaperStatus by remember { mutableStateOf<String?>(null) }
            var seenLock by remember { mutableIntStateOf(lockSignal) }
            if (seenLock != lockSignal) {
                seenLock = lockSignal
                locked = true
            }
            // Turning the option off clears the lock, so enabling it later never locks you out mid-edit.
            LaunchedEffect(home.loaded, home.settings.lockStyleEnabled) {
                if (home.loaded && !home.settings.lockStyleEnabled) locked = false
            }

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

            val showLock = home.loaded && screen == Screen.HOME &&
                ((home.settings.lockStyleEnabled && locked) || previewingLock)
            when {
                showLock -> LockStyleContent(home, onUnlock = { locked = false; previewingLock = false })
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
                    AppTheme(home.settings) {
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
                        override fun onTheme(theme: ThemeBundle) { customiseVm.applyTheme(theme) }
                        override fun onLayout(layout: HomeLayout) { customiseVm.setLayout(layout) }
                        override fun onLockStyle(enabled: Boolean) { customiseVm.setLockStyle(enabled) }
                        override fun onBackdrop(backdrop: Backdrop) { customiseVm.setBackdrop(backdrop) }
                        override fun onPreviewLock() { previewingLock = true; screen = Screen.HOME }
                        override fun onSetLockWallpaper() {
                            lockWallpaperStatus = "Applying\u2026"
                            lifecycleScope.launch {
                                val result = LockWallpaper.apply(context.applicationContext, home.settings)
                                lockWallpaperStatus = if (result.isSuccess) "Applied. Lock your phone to see it."
                                else "Could not apply: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                            }
                        }
                        override fun onReset() { customiseVm.reset() }
                    }
                    CustomiseContent(home.settings, actions, onBack = { screen = Screen.HOME }, lockWallpaperStatus = lockWallpaperStatus)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Screen off while in front: show the lock-style screen again next time.
        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) lockSignal++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeSignal++
    }

    override fun onResume() {
        super.onResume()
        (application as LauncherApplication).container.isDefaultLauncher.value = DefaultLauncherCheck.isDefault(this)
    }

    private fun startSafely(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
    }
}
