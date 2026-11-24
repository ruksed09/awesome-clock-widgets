package com.versity.launcher.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.versity.launcher.R

/**
 * Base for the clock widgets. The layouts use TextClock, which the home screen keeps current by itself,
 * so these only need to (re)attach the layout and the tap action. Tapping opens the phone's clock app.
 */
abstract class ClockWidgetProvider(@LayoutRes private val layout: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, layout).apply {
            setOnClickPendingIntent(R.id.widget_root, openClock(context))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun openClock(context: Context): PendingIntent {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

class CelestialTimeWidget : ClockWidgetProvider(R.layout.widget_celestial_time)

class CelestialTimeDateWidget : ClockWidgetProvider(R.layout.widget_celestial_time_date)

/** The clock widgets this app offers, as listed in Customise > Widgets. */
enum class ClockWidgetKind(
    val title: String,
    val description: String,
    val provider: Class<out AppWidgetProvider>,
    val showsDate: Boolean,
) {
    CELESTIAL_TIME("Celestial time", "Serif time with a starlit line", CelestialTimeWidget::class.java, false),
    CELESTIAL_TIME_DATE("Celestial time and date", "Serif time, starlit line and the date", CelestialTimeDateWidget::class.java, true),
}
