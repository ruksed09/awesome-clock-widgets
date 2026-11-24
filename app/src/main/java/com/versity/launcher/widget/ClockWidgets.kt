package com.versity.launcher.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.versity.launcher.R

/**
 * Base for the clock widgets. The layouts use TextClock, which the home screen keeps current by itself,
 * and auto-size their text to the cell, so these only attach the layout, the tap action and a date
 * format that fits the widget's width. Tapping opens the phone's clock app.
 */
abstract class ClockWidgetProvider(@LayoutRes private val layout: Int, private val showsDate: Boolean) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, manager.getAppWidgetOptions(it))) }
    }

    /** Resizing changes the width, so the date format is chosen again. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle?) {
        manager.updateAppWidget(id, views(context, options))
    }

    private fun views(context: Context, options: Bundle?): RemoteViews = RemoteViews(context.packageName, layout).apply {
        setOnClickPendingIntent(R.id.widget_root, openClock(context))
        if (showsDate) {
            val widthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
            // Narrow cells get "Wed · 11 Nov" so the date never clips; wider ones the full date.
            val format = if (widthDp in 1..219) "EEE · d MMM" else "EEEE  ·  d MMMM"
            setCharSequence(R.id.widget_date, "setFormat12Hour", format)
            setCharSequence(R.id.widget_date, "setFormat24Hour", format)
        }
    }

    private fun openClock(context: Context): PendingIntent {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

class CelestialTimeWidget : ClockWidgetProvider(R.layout.widget_celestial_time, showsDate = false)

class CelestialTimeDateWidget : ClockWidgetProvider(R.layout.widget_celestial_time_date, showsDate = true)

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
