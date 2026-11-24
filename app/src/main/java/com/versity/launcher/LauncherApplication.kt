package com.versity.launcher

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.versity.launcher.data.AppsRepository
import com.versity.launcher.data.LauncherAppsSource
import com.versity.launcher.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.versity.launcher.system.UpdateManager

private val Context.settingsDataStore by preferencesDataStore(name = "launcher_settings")

/** Manual DI for v1: one place that builds the repositories. */
class AppContainer(app: Application) {
    val settingsRepository = SettingsRepository(app.settingsDataStore)
    val appsRepository = AppsRepository(LauncherAppsSource(app))

    /** Updated from MainActivity.onResume; lives here so it survives rotation. */
    val isDefaultLauncher = MutableStateFlow(true)

    /** Checks GitHub for new releases; lives here so a download carries on across screens. */
    val updates = UpdateManager(app, CoroutineScope(SupervisorJob() + Dispatchers.Default))
}

class LauncherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
