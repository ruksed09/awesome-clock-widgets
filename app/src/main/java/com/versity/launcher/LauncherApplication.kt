package com.versity.launcher

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.versity.launcher.data.AppsRepository
import com.versity.launcher.data.LauncherAppsSource
import com.versity.launcher.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

private val Context.settingsDataStore by preferencesDataStore(name = "launcher_settings")

/** Manual DI for v1: one place that builds the repositories. */
class AppContainer(app: Application) {
    val settingsRepository = SettingsRepository(app.settingsDataStore)
    val appsRepository = AppsRepository(LauncherAppsSource(app))

    /** Updated from MainActivity.onResume; lives here so it survives rotation. */
    val isDefaultLauncher = MutableStateFlow(true)
}

class LauncherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
