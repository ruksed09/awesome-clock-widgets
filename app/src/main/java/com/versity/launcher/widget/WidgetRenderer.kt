package com.versity.launcher.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Icon
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.versity.launcher.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Turns a [WidgetSpec] into RemoteViews. The same views serve the phone's home screen, the studio's live
 * preview and widgets on Versity's own home screen, so all three always match. Time is kept by TextClock
 * and AnalogClock, so no widget ever needs an update alarm (the month view is redrawn after midnight).
 */
object WidgetRenderer {
    private val TextRows = listOf(
        R.id.w_kicker, R.id.w_title, R.id.w_time, R.id.w_hour, R.id.w_minute, R.id.w_day, R.id.w_date, R.id.w_date_caps,
    )

    fun build(context: Context, spec: WidgetSpec, widthDp: Int, heightDp: Int, locale: Locale = Locale.getDefault()): RemoteViews {
        val views = when {
            spec.type == WidgetType.ANALOGUE -> analogue(context, spec, widthDp, heightDp)
            spec.style == WidgetStyle.MONTH -> month(context, spec, widthDp, heightDp, locale)
            else -> text(context, spec, locale)
        }
        panel(context, views, spec)
        return views
    }

    @LayoutRes
    fun textLayout(typeface: String): Int = when (typeface) {
        "inter_bold" -> R.layout.widget_text_inter_bold
        "outfit_light" -> R.layout.widget_text_outfit_light
        "outfit_black" -> R.layout.widget_text_outfit_black
        "space_grotesk" -> R.layout.widget_text_space_grotesk
        "syne_bold" -> R.layout.widget_text_syne_bold
        "josefin_light" -> R.layout.widget_text_josefin_light
        "cormorant_light" -> R.layout.widget_text_cormorant_light
        "playfair" -> R.layout.widget_text_playfair
        "roboto_mono" -> R.layout.widget_text_roboto_mono
        else -> R.layout.widget_text_inter_thin
    }

    // ------------------------------------------------------------ Text widgets

    private fun text(context: Context, spec: WidgetSpec, locale: Locale): RemoteViews {
        val views = RemoteViews(context.packageName, textLayout(spec.typeface))
        val text = spec.textColor.toInt()
        val accent = spec.accentColor.toInt()
        val soft = withAlpha(text, 0xCC)
        val datePattern = bestPattern(locale, spec.dateStyle.skeleton)

        fun show(id: Int, color: Int, format: Pair<CharSequence, CharSequence>) {
            views.setViewVisibility(id, View.VISIBLE)
            views.setTextColor(id, color)
            setFormat(views, id, format)
        }
        fun show(id: Int, color: Int, pattern: CharSequence) = show(id, color, pattern to pattern)

        when (spec.style) {
            WidgetStyle.LINE -> show(R.id.w_time, text, timeFormat(spec))
            WidgetStyle.ORNAMENT -> { show(R.id.w_time, text, timeFormat(spec)); ornament(views, spec) }
            WidgetStyle.STACKED -> stacked(views, spec)
            WidgetStyle.ORNAMENT_DATE -> {
                show(R.id.w_time, text, timeFormat(spec)); ornament(views, spec); show(R.id.w_date_caps, soft, datePattern)
            }
            WidgetStyle.EDITORIAL -> { show(R.id.w_kicker, accent, datePattern); show(R.id.w_time, text, timeFormat(spec)) }
            WidgetStyle.COMPACT -> { show(R.id.w_time, text, timeFormat(spec)); show(R.id.w_date, soft, datePattern) }
            WidgetStyle.STACKED_DATE -> { stacked(views, spec); show(R.id.w_date_caps, soft, datePattern) }
            WidgetStyle.DAY_CARD -> {
                show(R.id.w_kicker, accent, "EEEE")
                show(R.id.w_day, if (spec.twoTone) accent else text, "d")
                show(R.id.w_date, soft, "MMMM")
            }
            WidgetStyle.DATE_LINE -> {
                show(R.id.w_title, text, "EEEE")
                show(R.id.w_date, if (spec.twoTone) accent else soft, bestPattern(locale, "dMMMMyyyy"))
            }
            else -> show(R.id.w_time, text, timeFormat(spec))
        }

        val gravity = horizontalGravity(spec.align) or Gravity.CENTER_VERTICAL
        views.setInt(R.id.w_content, "setGravity", gravity)
        views.setInt(R.id.w_ornament_row, "setGravity", gravity)
        // TextView gravity can only be changed from outside the app on Android 12 and newer.
        if (Build.VERSION.SDK_INT >= 31) TextRows.forEach { views.setInt(it, "setGravity", gravity) }
        return views
    }

    private fun stacked(views: RemoteViews, spec: WidgetSpec) {
        listOf(R.id.w_hour, R.id.w_minute).forEach { views.setViewVisibility(it, View.VISIBLE) }
        views.setTextColor(R.id.w_hour, spec.textColor.toInt())
        views.setTextColor(R.id.w_minute, (if (spec.twoTone) spec.accentColor else spec.textColor).toInt())
        val (h12, h24) = when (spec.timeFormat) {
            TimeFormat.SYSTEM -> "hh" to "HH"
            TimeFormat.H12 -> "hh" to "hh"
            TimeFormat.H24 -> "HH" to "HH"
        }
        views.setCharSequence(R.id.w_hour, "setFormat12Hour", h12)
        views.setCharSequence(R.id.w_hour, "setFormat24Hour", h24)
        setFormat(views, R.id.w_minute, "mm" to "mm")
    }

    private fun ornament(views: RemoteViews, spec: WidgetSpec) {
        views.setViewVisibility(R.id.w_ornament_row, View.VISIBLE)
        views.setImageViewResource(R.id.w_ornament, ornamentDrawable(spec.ornament))
        views.setInt(R.id.w_ornament, "setColorFilter", spec.accentColor.toInt())
    }

    fun ornamentDrawable(o: WidgetOrnament): Int = when (o) {
        WidgetOrnament.STAR -> R.drawable.wo_star
        WidgetOrnament.LINE -> R.drawable.wo_line
        WidgetOrnament.MOON -> R.drawable.wo_moon
    }

    /** The time pattern pair for TextClock, with the minutes in the accent when two-tone is on. */
    private fun timeFormat(spec: WidgetSpec): Pair<CharSequence, CharSequence> {
        fun pattern(hours: String): CharSequence {
            if (!spec.twoTone) return "$hours:mm"
            return SpannableStringBuilder("$hours:mm").apply {
                setSpan(ForegroundColorSpan(spec.accentColor.toInt()), length - 2, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }
        }
        return when (spec.timeFormat) {
            TimeFormat.SYSTEM -> pattern("h") to pattern("HH")
            TimeFormat.H12 -> pattern("h") to pattern("h")
            TimeFormat.H24 -> pattern("HH") to pattern("HH")
        }
    }

    private fun setFormat(views: RemoteViews, id: Int, format: Pair<CharSequence, CharSequence>) {
        views.setCharSequence(id, "setFormat12Hour", format.first)
        views.setCharSequence(id, "setFormat24Hour", format.second)
    }

    /** The locale's own order and punctuation for a skeleton like "EEEEdMMMM". */
    fun bestPattern(locale: Locale, skeleton: String): String = DateFormat.getBestDateTimePattern(locale, skeleton)

    // ------------------------------------------------------------ Analogue

    private fun analogue(context: Context, spec: WidgetSpec, widthDp: Int, heightDp: Int): RemoteViews {
        // Hands come in light and dark ink, so they read on any face even where they cannot be tinted.
        val light = luminance(spec.textColor) > 0.5
        val layout = when (spec.hands) {
            HandStyle.CLASSIC -> if (light) R.layout.widget_analog_classic_light else R.layout.widget_analog_classic_dark
            HandStyle.SLIM -> if (light) R.layout.widget_analog_slim_light else R.layout.widget_analog_slim_dark
            HandStyle.BOLD -> if (light) R.layout.widget_analog_bold_light else R.layout.widget_analog_bold_dark
        }
        val views = RemoteViews(context.packageName, layout)
        val density = context.resources.displayMetrics.density
        val side = (minOf(widthDp, heightDp) * density).toInt()
        views.setImageViewBitmap(R.id.w_dial, AnalogDial.render(context, spec, side))
        // Exact hand colours and the seconds hand need Android 12's RemoteViews methods.
        if (Build.VERSION.SDK_INT >= 31) {
            views.setColorStateList(R.id.w_analog, "setHourHandTintList", ColorStateList.valueOf(spec.textColor.toInt()))
            views.setColorStateList(R.id.w_analog, "setMinuteHandTintList", ColorStateList.valueOf((if (spec.twoTone) spec.accentColor else spec.textColor).toInt()))
            if (spec.seconds) {
                views.setIcon(R.id.w_analog, "setSecondHand", Icon.createWithResource(context, R.drawable.wa_hand_second))
                views.setColorStateList(R.id.w_analog, "setSecondHandTintList", ColorStateList.valueOf(spec.accentColor.toInt()))
            }
        }
        return views
    }

    // ------------------------------------------------------------ Month

    private fun month(context: Context, spec: WidgetSpec, widthDp: Int, heightDp: Int, locale: Locale): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_calendar)
        val density = context.resources.displayMetrics.density
        val inset = (if (spec.background == WidgetBackground.NONE) 12 else 28)
        val w = ((widthDp - inset).coerceAtLeast(80) * density).roundToInt()
        val h = ((heightDp - inset).coerceAtLeast(80) * density).roundToInt()
        views.setImageViewBitmap(R.id.w_calendar, MonthCalendar.render(context, spec, w, h, locale = locale))
        return views
    }

    // ------------------------------------------------------------ Panel

    private fun panel(context: Context, views: RemoteViews, spec: WidgetSpec) {
        fun px(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).roundToInt()
        val panel = spec.background != WidgetBackground.NONE
        if (panel) {
            views.setViewVisibility(R.id.w_bg, View.VISIBLE)
            views.setImageViewResource(R.id.w_bg, fillDrawable(spec.corner))
            val tint = if (spec.background == WidgetBackground.GLASS) 0xFFFFFFFF.toInt() else spec.backgroundColor.toInt()
            views.setInt(R.id.w_bg, "setColorFilter", tint)
            views.setInt(R.id.w_bg, "setImageAlpha", alpha255(spec.opacity))
        }
        if (spec.border) {
            views.setViewVisibility(R.id.w_border, View.VISIBLE)
            views.setImageViewResource(R.id.w_border, strokeDrawable(spec.corner))
            val light = luminance(spec.textColor) > 0.5
            views.setInt(R.id.w_border, "setColorFilter", if (light) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            views.setInt(R.id.w_border, "setImageAlpha", if (light) 0x55 else 0x33)
        }
        // A panel needs breathing room inside its edge; bare text can use nearly the whole cell.
        val pad = if (panel || spec.border) px(if (spec.type == WidgetType.ANALOGUE) 8f else 14f) else px(4f)
        views.setViewPadding(R.id.w_content, pad, pad, pad, pad)
    }

    fun fillDrawable(corner: WidgetCorner): Int = when (corner) {
        WidgetCorner.SQUARE -> R.drawable.wbg_fill_r0
        WidgetCorner.SMALL -> R.drawable.wbg_fill_r12
        WidgetCorner.MEDIUM -> R.drawable.wbg_fill_r20
        WidgetCorner.LARGE -> R.drawable.wbg_fill_r28
    }

    private fun strokeDrawable(corner: WidgetCorner): Int = when (corner) {
        WidgetCorner.SQUARE -> R.drawable.wbg_stroke_r0
        WidgetCorner.SMALL -> R.drawable.wbg_stroke_r12
        WidgetCorner.MEDIUM -> R.drawable.wbg_stroke_r20
        WidgetCorner.LARGE -> R.drawable.wbg_stroke_r28
    }

    private fun horizontalGravity(align: WidgetAlign) = when (align) {
        WidgetAlign.START -> Gravity.START
        WidgetAlign.CENTER -> Gravity.CENTER_HORIZONTAL
        WidgetAlign.END -> Gravity.END
    }

    private fun alpha255(percent: Int) = (percent.coerceIn(0, 100) * 255 / 100)

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)

    fun luminance(argb: Long): Double {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
    }
}
