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
        Widgets.changed()
        if (type == WidgetType.DATE) Widgets.scheduleMidnight(context)
        if (type == WidgetType.COUNTDOWN) Widgets.scheduleCountdown(context)
    }

    /** Resizing changes the cell, which the month view is drawn for. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle?) {
        Widgets.update(context, manager, id, type)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val store = WidgetStore(context)
        ids.forEach(store::removeSystem)
        Widgets.changed()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_BOOT_COMPLETED,
            Widgets.ACTION_REFRESH ->
                Widgets.updateAll(context, type)
            Widgets.ACTION_TURN -> {
                // The spinner's hub: turn the wheel one step clockwise and redraw it.
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val store = WidgetStore(context)
                    val spec = store.system(id, type)
                    store.saveSystem(id, spec.copy(turn = (spec.turn + 1).mod(CollectionArt.SPIN_SLOTS)))
                    AppWidgetManager.getInstance(context)?.let { Widgets.update(context, it, id, type) }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }
}

class CelestialTimeWidget : VersityWidgetProvider(WidgetType.DIGITAL)
class CelestialTimeDateWidget : VersityWidgetProvider(WidgetType.CLOCK_DATE)
class AnalogueClockWidget : VersityWidgetProvider(WidgetType.ANALOGUE)
class DateWidget : VersityWidgetProvider(WidgetType.DATE)
class CountdownWidget : VersityWidgetProvider(WidgetType.COUNTDOWN)
class RadialLauncherWidget : VersityWidgetProvider(WidgetType.LAUNCHER)
class AppShortcutWidget : VersityWidgetProvider(WidgetType.SHORTCUT)
class BadgeWidget : VersityWidgetProvider(WidgetType.BADGE)

/** Receives the home screen's confirmation of a pinned widget and gives it the look designed in the studio. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val token = intent.getStringExtra(Widgets.EXTRA_DRAFT) ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val spec = WidgetStore(context).takeDraft(token) ?: return
        WidgetStore(context).saveSystem(id, spec)
        Widgets.update(context, AppWidgetManager.getInstance(context), id, spec.type)
        Widgets.changed()
    }
}

/** Placed widgets, pinning and refreshes. */
object Widgets {
    const val ACTION_REFRESH = "com.versity.launcher.widget.REFRESH"
    const val ACTION_TURN = "com.versity.launcher.widget.TURN"
    const val EXTRA_DRAFT = "com.versity.launcher.widget.DRAFT"

    fun providerClass(type: WidgetType): Class<out VersityWidgetProvider> = when (type) {
        WidgetType.DIGITAL -> CelestialTimeWidget::class.java
        WidgetType.CLOCK_DATE -> CelestialTimeDateWidget::class.java
        WidgetType.ANALOGUE -> AnalogueClockWidget::class.java
        WidgetType.DATE -> DateWidget::class.java
        WidgetType.COUNTDOWN -> CountdownWidget::class.java
        WidgetType.LAUNCHER -> RadialLauncherWidget::class.java
        WidgetType.SHORTCUT -> AppShortcutWidget::class.java
        WidgetType.BADGE -> BadgeWidget::class.java
    }

    fun typeOf(provider: ComponentName?): WidgetType? =
        WidgetType.entries.firstOrNull { providerClass(it).name == provider?.className }

    /**
     * Goes up whenever a widget of ours is placed, removed or restyled on the phone's home screen, so
     * screens listing them can refresh while they are open.
     */
    val changes = androidx.compose.runtime.mutableIntStateOf(0)

    fun changed() { changes.intValue++ }

    /**
     * Widgets of ours on the phone's home screen, newest first. Android also counts widgets a home
     * screen reserved but never showed (a placement that was abandoned half way), so only widgets the
     * home screen has laid out, or that have a look saved, are counted.
     */
    fun placed(context: Context): List<Pair<Int, WidgetType>> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        val store = WidgetStore(context)
        return WidgetType.entries.flatMap { type ->
            runCatching { manager.getAppWidgetIds(ComponentName(context, providerClass(type))) }.getOrDefault(IntArray(0)).map { it to type }
        }.filter { (id, _) -> isShown(manager, id) || store.hasSystem(id) }
            .sortedByDescending { it.first }
    }

    /** Whether the widget [id] is still on the phone's home screen. */
    fun isPlaced(context: Context, id: Int): Boolean = placed(context).any { it.first == id }

    private fun isShown(manager: AppWidgetManager, id: Int): Boolean {
        val options = runCatching { manager.getAppWidgetOptions(id) }.getOrNull() ?: return false
        return options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) > 0 || options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH) > 0
    }

    fun update(context: Context, manager: AppWidgetManager, id: Int, type: WidgetType) {
        val spec = WidgetStore(context).system(id, type)
        val options = manager.getAppWidgetOptions(id)
        val w = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 } ?: 180
        val h = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)?.takeIf { it > 0 } ?: 180
        val views = WidgetRenderer.build(context, spec, w, h, appWidgetId = id)
        // Collection widgets attach their own taps (apps); the clocks open the clock or calendar app.
        if (!type.collection) views.setOnClickPendingIntent(com.versity.launcher.R.id.w_root, clickIntent(context, type))
        manager.updateAppWidget(id, views)
    }

    fun updateAll(context: Context, type: WidgetType) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        manager.getAppWidgetIds(ComponentName(context, providerClass(type))).forEach { update(context, manager, it, type) }
        if (type == WidgetType.DATE) scheduleMidnight(context)
        if (type == WidgetType.COUNTDOWN) scheduleCountdown(context)
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

    /** A widget the home screen was asked to place: what to look for when the answer comes back. */
    data class PendingPin(val token: String, val type: WidgetType, val before: Set<Int>)

    /**
     * Asks the current home app to place a widget with [spec]'s look. The look is kept as a draft until
     * the home app confirms, then moved to the new widget. Null when the home app can't pin widgets.
     *
     * The request carries a preview of the design, so the home screen's confirmation shows the widget as
     * it will look. A preview too large to send is left out rather than losing the request.
     */
    fun requestPin(context: Context, spec: WidgetSpec): PendingPin? {
        val manager = AppWidgetManager.getInstance(context) ?: return null
        if (!manager.isRequestPinAppWidgetSupported) return null
        val token = UUID.randomUUID().toString()
        val before = placedIds(context, manager)
        WidgetStore(context).saveDraft(token, spec)
        val callback = PendingIntent.getBroadcast(
            context, token.hashCode(),
            Intent(context, WidgetPinnedReceiver::class.java).putExtra(EXTRA_DRAFT, token),
            // The home app adds the new widget's id to this intent, so it must stay mutable.
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
        )
        val provider = ComponentName(context, providerClass(spec.type))
        val preview = runCatching {
            val (w, h) = if (spec.aspect >= 1.6f) 220 to 100 else 120 to 120
            Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, WidgetRenderer.build(context, spec, w, h)) }
        }.getOrNull()
        val asked = runCatching { manager.requestPinAppWidget(provider, preview, callback) }.getOrNull()
            ?: runCatching { manager.requestPinAppWidget(provider, null, callback) }.getOrDefault(false)
        if (!asked) {
            WidgetStore(context).takeDraft(token)
            return null
        }
        return PendingPin(token, spec.type, before)
    }

    /**
     * Whether the home screen placed the widget asked for in [pin]: it confirmed (and took the look), or a
     * new widget of that family appeared.
     */
    fun wasPinned(context: Context, pin: PendingPin): Boolean {
        if (!WidgetStore(context).hasDraft(pin.token)) return true
        val manager = AppWidgetManager.getInstance(context) ?: return false
        val now = runCatching { manager.getAppWidgetIds(ComponentName(context, providerClass(pin.type))) }.getOrDefault(IntArray(0))
        return now.any { it !in pin.before }
    }

    private fun placedIds(context: Context, manager: AppWidgetManager): Set<Int> = WidgetType.entries.flatMap { type ->
        runCatching { manager.getAppWidgetIds(ComponentName(context, providerClass(type))).toList() }.getOrDefault(emptyList())
    }.toSet()

    /**
     * Redraws countdowns when the soonest one's day count drops (its timer reaches zero). The timer keeps
     * itself current in between; a short window is plenty, and needs no exact-alarm permission.
     */
    fun scheduleCountdown(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val manager = AppWidgetManager.getInstance(context) ?: return
        val store = WidgetStore(context)
        val now = System.currentTimeMillis()
        val next = manager.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))
            .map { Collection.state(Collection.target(store.system(it, WidgetType.COUNTDOWN), now = now), now) }
            .filterNot { it.done }
            .minOfOrNull { it.timerEnd } ?: return
        val intent = Intent(context, CountdownWidget::class.java).setAction(ACTION_REFRESH)
        val pending = PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching { alarms.setWindow(AlarmManager.RTC, next + 500, 30_000L, pending) }
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
