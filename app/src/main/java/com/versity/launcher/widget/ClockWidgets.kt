package com.versity.launcher.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * One provider per widget family. Each widget's look is in [WidgetStore], keyed by its id; a widget
 * without a saved look uses its family's default. The first two keep their original class names so
 * widgets placed by earlier versions keep working (and keep their Celestial look, the default).
 */
abstract class VersityWidgetProvider(val type: WidgetType) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { Widgets.update(context, manager, it, type) }
        if (type == WidgetType.DATE) Widgets.scheduleMidnight(context)
    }

    /** Resizing changes the cell, which the month view is drawn for. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle?) {
        Widgets.update(context, manager, id, type)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val store = WidgetStore(context)
        ids.forEach(store::removeSystem)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED, Widgets.ACTION_REFRESH ->
                Widgets.updateAll(context, type)
            else -> super.onReceive(context, intent)
        }
    }
}

class CelestialTimeWidget : VersityWidgetProvider(WidgetType.DIGITAL)
class CelestialTimeDateWidget : VersityWidgetProvider(WidgetType.CLOCK_DATE)
class AnalogueClockWidget : VersityWidgetProvider(WidgetType.ANALOGUE)
class DateWidget : VersityWidgetProvider(WidgetType.DATE)

/** Receives the home screen's confirmation of a pinned widget and gives it the look designed in the studio. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val token = intent.getStringExtra(Widgets.EXTRA_DRAFT) ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val spec = WidgetStore(context).takeDraft(token) ?: return
        WidgetStore(context).saveSystem(id, spec)
        Widgets.update(context, AppWidgetManager.getInstance(context), id, spec.type)
    }
}

/** Placed widgets, pinning and refreshes. */
object Widgets {
    const val ACTION_REFRESH = "com.versity.launcher.widget.REFRESH"
    const val EXTRA_DRAFT = "com.versity.launcher.widget.DRAFT"

    fun providerClass(type: WidgetType): Class<out VersityWidgetProvider> = when (type) {
        WidgetType.DIGITAL -> CelestialTimeWidget::class.java
        WidgetType.CLOCK_DATE -> CelestialTimeDateWidget::class.java
        WidgetType.ANALOGUE -> AnalogueClockWidget::class.java
        WidgetType.DATE -> DateWidget::class.java
    }

    fun typeOf(provider: ComponentName?): WidgetType? =
        WidgetType.entries.firstOrNull { providerClass(it).name == provider?.className }

    /** Widgets of ours on the phone's home screen, newest first. */
    fun placed(context: Context): List<Pair<Int, WidgetType>> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        return WidgetType.entries.flatMap { type ->
            runCatching { manager.getAppWidgetIds(ComponentName(context, providerClass(type))) }.getOrDefault(IntArray(0)).map { it to type }
        }.sortedByDescending { it.first }
    }

    fun update(context: Context, manager: AppWidgetManager, id: Int, type: WidgetType) {
        val spec = WidgetStore(context).system(id, type)
        val options = manager.getAppWidgetOptions(id)
        val w = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 } ?: 180
        val h = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)?.takeIf { it > 0 } ?: 180
        val views = WidgetRenderer.build(context, spec, w, h)
        views.setOnClickPendingIntent(com.versity.launcher.R.id.w_root, clickIntent(context, type))
        manager.updateAppWidget(id, views)
    }

    fun updateAll(context: Context, type: WidgetType) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        manager.getAppWidgetIds(ComponentName(context, providerClass(type))).forEach { update(context, manager, it, type) }
        if (type == WidgetType.DATE) scheduleMidnight(context)
    }

    /** Time widgets open the clock app; date widgets open the calendar. */
    fun clickIntent(context: Context, type: WidgetType): PendingIntent {
        val intent = if (type == WidgetType.DATE) {
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        } else {
            Intent(AlarmClock.ACTION_SHOW_ALARMS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, type.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Asks the current home app to place a widget with [spec]'s look. The look is kept as a draft until
     * the home app confirms, then moved to the new widget. False when the home app can't pin widgets.
     */
    fun requestPin(context: Context, spec: WidgetSpec): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        val token = UUID.randomUUID().toString()
        WidgetStore(context).saveDraft(token, spec)
        val callback = PendingIntent.getBroadcast(
            context, token.hashCode(),
            Intent(context, WidgetPinnedReceiver::class.java).putExtra(EXTRA_DRAFT, token),
            // The home app adds the new widget's id to this intent, so it must stay mutable.
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
        )
        return runCatching { manager.requestPinAppWidget(ComponentName(context, providerClass(spec.type)), null, callback) }.getOrDefault(false)
    }

    /** Redraws the month view just after midnight. Inexact is fine: a minute or two late is invisible. */
    fun scheduleMidnight(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val zone = ZoneId.systemDefault()
        val at = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 30_000L
        val intent = Intent(context, DateWidget::class.java).setAction(ACTION_REFRESH)
        val pending = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching { alarms.set(AlarmManager.RTC, at, pending) }
    }
}
