package com.versity.launcher.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.versity.launcher.domain.ThemePresets
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.min

/**
 * Draws the month view of the Date widget: the month name, weekday initials and the days, with today
 * on an accent disc. Weeks start on the locale's first day. Drawn once and redrawn after midnight.
 */
object MonthCalendar {
    fun render(
        context: Context,
        spec: WidgetSpec,
        widthPx: Int,
        heightPx: Int,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
    ): Bitmap {
        // RemoteViews bitmaps count against a memory budget, so cap the size; fitCenter scales it up.
        val scale = min(1f, 900f / maxOf(widthPx, heightPx))
        val w = (widthPx * scale).toInt().coerceAtLeast(60)
        val h = (heightPx * scale).toInt().coerceAtLeast(60)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val typeface = WidgetTypefaces.of(spec.typeface)
        val base: Typeface = runCatching { ResourcesCompat.getFont(context, ThemePresets.font(typeface.fontId).resId) }.getOrNull() ?: Typeface.DEFAULT
        fun paint(weight: Int, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = base
            fontVariationSettings = "'wght' $weight"
            fontFeatureSettings = "lnum, tnum"
            this.color = color
        }

        val text = spec.textColor.toInt()
        val accent = spec.accentColor.toInt()
        val first = today.withDayOfMonth(1)
        val weekStart: DayOfWeek = WeekFields.of(locale).firstDayOfWeek
        val lead = (first.dayOfWeek.value - weekStart.value + 7) % 7
        val weeks = (lead + today.lengthOfMonth() + 6) / 7

        // Rows: month header (1.5 units), weekday initials (1), then the weeks (1 each).
        val unit = h / (2.5f + weeks)
        val cell = w / 7f
        val size = min(cell, unit) * 0.46f
        val pad = cell * 0.5f

        val header = paint(maxOf(typeface.weight, 500).coerceAtMost(700), accent).apply { textSize = size * 1.15f }
        val monthName = today.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
        val headerY = unit * 0.95f
        when (spec.align) {
            WidgetAlign.START -> { header.textAlign = Paint.Align.LEFT; canvas.drawText(monthName, pad - size * 0.4f, headerY, header) }
            WidgetAlign.CENTER -> { header.textAlign = Paint.Align.CENTER; canvas.drawText(monthName, w / 2f, headerY, header) }
            WidgetAlign.END -> { header.textAlign = Paint.Align.RIGHT; canvas.drawText(monthName, w - pad + size * 0.4f, headerY, header) }
        }

        val initials = paint(500, (text and 0x00FFFFFF) or (0x8C shl 24)).apply { textSize = size * 0.8f; textAlign = Paint.Align.CENTER }
        for (i in 0 until 7) {
            val day = weekStart.plus(i.toLong())
            val label = day.getDisplayName(TextStyle.NARROW_STANDALONE, locale)
            canvas.drawText(label, cell * (i + 0.5f), unit * 2.05f, initials)
        }

        val days = paint(typeface.weight.coerceIn(300, 600), text).apply { textSize = size; textAlign = Paint.Align.CENTER }
        val todayInk = paint(600, if (WidgetRenderer.luminance(spec.accentColor) > 0.6) 0xFF111114.toInt() else 0xFFFFFFFF.toInt())
            .apply { textSize = size; textAlign = Paint.Align.CENTER }
        val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val metrics = days.fontMetrics
        val centreOffset = -(metrics.ascent + metrics.descent) / 2f
        for (d in 1..today.lengthOfMonth()) {
            val index = lead + d - 1
            val col = index % 7
            val row = index / 7
            val cx = cell * (col + 0.5f)
            val cy = unit * (2.5f + row + 0.5f)
            if (d == today.dayOfMonth) {
                canvas.drawCircle(cx, cy, min(cell, unit) * 0.42f, disc)
                canvas.drawText(d.toString(), cx, cy + centreOffset, todayInk)
            } else {
                canvas.drawText(d.toString(), cx, cy + centreOffset, days)
            }
        }
        return bitmap
    }
}
