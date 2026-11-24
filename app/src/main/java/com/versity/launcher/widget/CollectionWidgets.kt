package com.versity.launcher.widget

import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.versity.launcher.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Where a countdown stands at a moment: whole days left, and when the day count next drops. */
data class CountdownState(val days: Long, val timerEnd: Long, val done: Boolean)

/** RemoteViews for the collection: countdown, radial launcher, app shortcut and badge. */
object Collection {
    private const val DAY = 24 * 60 * 60 * 1000L

    /** The moment counted down to; unset means midnight at the start of next year. */
    fun target(spec: WidgetSpec, zone: ZoneId = ZoneId.systemDefault(), now: Long = System.currentTimeMillis()): Long =
        spec.target.takeIf { it > 0 } ?: LocalDate.ofInstant(Instant.ofEpochMilli(now), zone).withDayOfYear(1).plusYears(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    /**
     * Whole days left, and the moment the hours-minutes-seconds part reaches zero (when the day count
     * drops by one). The timer always shows less than a day, as in the reference.
     */
    fun state(target: Long, now: Long): CountdownState {
        val left = target - now
        if (left <= 0) return CountdownState(0, target, done = true)
        val days = left / DAY
        return CountdownState(days, target - days * DAY, done = false)
    }

    fun countdown(context: Context, spec: WidgetSpec, locale: Locale): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_countdown)
        val now = System.currentTimeMillis()
        val target = target(spec, now = now)
        val st = state(target, now)
        val text = spec.textColor.toInt()
        val soft = (text and 0x00FFFFFF) or (0x99 shl 24)
        val accent = spec.accentColor.toInt()
        val title = spec.title.ifBlank { "Countdown" }
        val date = DateTimeFormatter.ofPattern(WidgetRenderer.bestPattern(locale, "EEEdMMMyyyy"), locale)
            .format(Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault()))
        views.setTextViewText(R.id.w_cd_title, title)
        views.setTextColor(R.id.w_cd_title, accent)
        views.setTextViewText(R.id.w_cd_until, if (st.done) "Since $date" else "Until $date")
        views.setTextColor(R.id.w_cd_until, soft)
        views.setContentDescription(R.id.w_root, if (st.done) "$title, $date" else "$title in ${st.days} days, $date")

        val big = spec.style == WidgetStyle.COUNTDOWN_BIG
        views.setViewVisibility(R.id.w_cd_row, if (big) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.w_cd_big, if (big) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.w_cd_big_label, if (big) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.w_cd_big, st.days.toString())
        views.setTextColor(R.id.w_cd_big, accent)
        views.setTextViewText(R.id.w_cd_big_label, when {
            st.done -> "Today"
            st.days == 1L -> "Day to go"
            else -> "Days to go"
        })
        views.setTextColor(R.id.w_cd_big_label, soft)

        views.setTextViewText(R.id.w_cd_days, st.days.toString())
        views.setTextColor(R.id.w_cd_days, accent)
        views.setTextViewText(R.id.w_cd_days_label, if (st.days == 1L) "Day" else "Days")
        views.setTextColor(R.id.w_cd_days_label, soft)
        views.setTextColor(R.id.w_cd_timer, text)
        // The Chronometer counts down on its own, so the app never wakes once a second.
        val base = SystemClock.elapsedRealtime() + (st.timerEnd - now)
        views.setChronometer(R.id.w_cd_timer, if (st.done) SystemClock.elapsedRealtime() else base, null, !st.done)
        views.setChronometerCountDown(R.id.w_cd_timer, true)

        // Pills: a step lighter than a solid panel, or a faint wash of the text colour over the wallpaper.
        val solid = spec.background == WidgetBackground.SOLID
        listOf(R.id.w_cd_days_bg, R.id.w_cd_timer_bg).forEach { id ->
            views.setImageViewResource(id, WidgetRenderer.fillDrawable(pillCorner(spec.corner)))
            views.setInt(id, "setColorFilter", if (solid) CollectionArt.lighten(spec.backgroundColor.toInt(), 0.06f) else text)
            views.setInt(id, "setImageAlpha", if (solid) 255 else 0x24)
        }
        return views
    }

    private fun pillCorner(c: WidgetCorner) = when (c) {
        WidgetCorner.SQUARE -> WidgetCorner.SQUARE
        WidgetCorner.SMALL -> WidgetCorner.SMALL
        else -> WidgetCorner.MEDIUM
    }

    fun radial(context: Context, spec: WidgetSpec, widthDp: Int, heightDp: Int, interactive: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_radial)
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).roundToInt().coerceAtLeast(1)
        val h = (heightDp * density).roundToInt().coerceAtLeast(1)
        val side = minOf(w, h)
        views.setImageViewBitmap(R.id.w_art, CollectionArt.radial(context, spec, side))
        // The image is centred square; pad the tap grid to that same square.
        views.setViewPadding(R.id.w_hit, (w - side) / 2, (h - side) / 2, (w - side) / 2, (h - side) / 2)
        val cells = listOf(R.id.w_hit_0, R.id.w_hit_1, R.id.w_hit_2, R.id.w_hit_3, R.id.w_hit_4)
        cells.forEachIndexed { i, id ->
            val app = spec.apps.getOrNull(i)
            views.setContentDescription(id, app?.let { CollectionArt.label(context, it) } ?: "Empty slot")
            if (interactive && app != null) open(context, app, i)?.let { views.setOnClickPendingIntent(id, it) }
        }
        return views
    }

    fun art(context: Context, spec: WidgetSpec, widthDp: Int, heightDp: Int, interactive: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_art)
        val density = context.resources.displayMetrics.density
        val side = (minOf(widthDp, heightDp) * density).roundToInt()
        val bitmap = if (spec.type == WidgetType.BADGE) CollectionArt.badge(context, spec, side) else CollectionArt.shortcut(context, spec, side)
        views.setImageViewBitmap(R.id.w_art, bitmap)
        val app = spec.apps.firstOrNull()
        views.setContentDescription(R.id.w_art, app?.let { CollectionArt.label(context, it) } ?: spec.type.title)
        if (interactive && app != null) open(context, app, 0)?.let { views.setOnClickPendingIntent(R.id.w_root, it) }
        return views
    }

    private fun open(context: Context, app: String, slot: Int): PendingIntent? {
        val intent = CollectionArt.launchIntent(context, app) ?: return null
        return PendingIntent.getActivity(
            context, (app.hashCode() * 31 + slot), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
