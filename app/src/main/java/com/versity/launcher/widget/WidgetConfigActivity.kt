package com.versity.launcher.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.versity.launcher.LauncherApplication
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.studio.StudioTheme
import com.versity.launcher.ui.theme.CappedFontScale
import com.versity.launcher.ui.studio.studioIsDark
import com.versity.launcher.ui.widgets.WidgetStudio

/**
 * Opened by the home screen when one of our widgets is placed (from the system widget picker) or
 * re-edited. Shows the widget studio for that widget; Save keeps the look and redraws the widget.
 * Closing without saving cancels a new placement, as Android expects.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        val manager = AppWidgetManager.getInstance(this)
        val type = Widgets.typeOf(manager?.getAppWidgetInfo(id)?.provider)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || manager == null || type == null) {
            finish()
            return
        }
        val store = WidgetStore(this)
        val existing = store.hasSystem(id)
        // A widget designed in the app a moment ago and sent to the home screen: some home screens still
        // open this screen while placing it. The design is already chosen, so it is applied at once;
        // showing the studio again would look like the add had failed, and closing it would drop the widget.
        if (!existing) store.recentDraft(type, withinMillis = JUST_DESIGNED)?.let { spec ->
            store.saveSystem(id, spec)
            store.dropDrafts(type)
            Widgets.update(this, manager, id, type)
            Widgets.changed()
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            finish()
            return
        }
        val initial = if (existing) store.system(id, type) else store.recentDraft(type) ?: WidgetSpec.default(type)
        val settings = (application as LauncherApplication).container.settingsRepository.settings

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent { CappedFontScale {
            val s by settings.collectAsState(initial = LauncherSettings.DEFAULT)
            StudioTheme(s.uiMode) {
                val dark = studioIsDark(s.uiMode)
                LaunchedEffect(dark) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                WidgetStudio(
                    initial = initial,
                    title = if (existing) "Edit widget" else "New widget",
                    confirmLabel = if (existing) "Save" else "Add widget",
                    compactConfirmLabel = if (existing) null else "Add",
                    wallpaperUri = s.wallpaperUri,
                    onConfirm = { spec ->
                        store.saveSystem(id, spec)
                        // The design this started from (if any) is now on a widget, so it is not offered again.
                        if (!existing) store.dropDrafts(type)
                        Widgets.update(this, manager, id, type)
                        Widgets.changed()
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        finish()
                    },
                    onClose = { finish() },
                )
            }
        } }
    }

    private companion object {
        /** How recent an in-app design must be to count as the one being placed now. */
        const val JUST_DESIGNED = 3 * 60_000L
    }
}
