package com.versity.launcher.widget

/** The four widget families offered in the widget studio and the system widget picker. */
enum class WidgetType(val title: String, val description: String) {
    DIGITAL("Digital clock", "One line, stacked or with an ornament"),
    CLOCK_DATE("Clock and date", "Time with the date, four layouts"),
    ANALOGUE("Analogue clock", "Three dials, optional seconds"),
    DATE("Date and calendar", "Day card, date line or month"),
}

/** Layouts within a family. Stored by name, so new ones can be added without breaking saved widgets. */
enum class WidgetStyle(val type: WidgetType, val label: String) {
    LINE(WidgetType.DIGITAL, "Line"),
    STACKED(WidgetType.DIGITAL, "Stacked"),
    ORNAMENT(WidgetType.DIGITAL, "Ornament"),
    ORNAMENT_DATE(WidgetType.CLOCK_DATE, "Ornament"),
    EDITORIAL(WidgetType.CLOCK_DATE, "Editorial"),
    COMPACT(WidgetType.CLOCK_DATE, "Compact"),
    STACKED_DATE(WidgetType.CLOCK_DATE, "Stacked"),
    DIAL_INDEX(WidgetType.ANALOGUE, "Index"),
    DIAL_MINIMAL(WidgetType.ANALOGUE, "Minimal"),
    DIAL_DOTS(WidgetType.ANALOGUE, "Dots"),
    DAY_CARD(WidgetType.DATE, "Day card"),
    DATE_LINE(WidgetType.DATE, "Date line"),
    MONTH(WidgetType.DATE, "Month"),
    ;

    companion object {
        fun of(type: WidgetType) = entries.filter { it.type == type }
    }
}

/** NONE: text straight on the wallpaper. GLASS: a translucent light panel with a hairline. SOLID: a colour panel. */
enum class WidgetBackground(val label: String) { NONE("None"), GLASS("Glass"), SOLID("Solid") }

enum class WidgetCorner(val label: String, val dp: Int) { SQUARE("Square", 0), SMALL("Small", 12), MEDIUM("Medium", 20), LARGE("Large", 28) }

enum class WidgetAlign(val label: String) { START("Left"), CENTER("Centre"), END("Right") }

/** SYSTEM follows the phone's 12/24-hour setting. */
enum class TimeFormat(val label: String) { SYSTEM("Phone"), H12("12-hour"), H24("24-hour") }

/** Date patterns as skeletons, so each locale orders them its own way. */
enum class DateStyle(val label: String, val skeleton: String) {
    FULL("Wednesday 11 November", "EEEEdMMMM"),
    SHORT("Wed 11 Nov", "EEEdMMM"),
    DAY_MONTH("11 November", "dMMMM"),
    WEEKDAY("Wednesday", "EEEE"),
}

enum class WidgetOrnament(val label: String) { STAR("Star"), LINE("Line"), MOON("Moon") }

/**
 * Everything that defines how one widget looks. Colours are opaque ARGB; [opacity] (0..100) applies to
 * the panel and the analogue face. Text is never dimmed except the date line, which uses 80%.
 */
data class WidgetSpec(
    val style: WidgetStyle,
    val typeface: String = WidgetTypefaces.DEFAULT,
    val textColor: Long = 0xFFFFFFFF,
    val accentColor: Long = 0xFFFFFFFF,
    /** Minutes (or the day number) in the accent colour. */
    val twoTone: Boolean = false,
    val ornament: WidgetOrnament = WidgetOrnament.STAR,
    val background: WidgetBackground = WidgetBackground.NONE,
    val backgroundColor: Long = 0xFF111114,
    val opacity: Int = 60,
    val corner: WidgetCorner = WidgetCorner.LARGE,
    val border: Boolean = false,
    val align: WidgetAlign = WidgetAlign.CENTER,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val dateStyle: DateStyle = DateStyle.FULL,
    /** Analogue: a filled disc behind the dial, in [backgroundColor] at [opacity]. */
    val dialFace: Boolean = false,
    /** Analogue: a seconds hand (Android 12 and newer). */
    val seconds: Boolean = false,
) {
    val type: WidgetType get() = style.type

    /** Whether the style shows the date, so the studio can hide date options that do nothing. */
    val showsDate: Boolean get() = style in setOf(
        WidgetStyle.ORNAMENT_DATE, WidgetStyle.EDITORIAL, WidgetStyle.COMPACT, WidgetStyle.STACKED_DATE, WidgetStyle.DATE_LINE,
    )
    val usesOrnament: Boolean get() = style == WidgetStyle.ORNAMENT || style == WidgetStyle.ORNAMENT_DATE
    val showsTime: Boolean get() = type == WidgetType.DIGITAL || type == WidgetType.CLOCK_DATE
    val usesTypeface: Boolean get() = type != WidgetType.ANALOGUE

    /** Width over height that suits this style; used for previews and widgets on Versity's home screen. */
    val aspect: Float get() = when (style) {
        WidgetStyle.LINE -> 2.6f
        WidgetStyle.STACKED -> 1.1f
        WidgetStyle.ORNAMENT -> 2.2f
        WidgetStyle.ORNAMENT_DATE, WidgetStyle.EDITORIAL -> 1.9f
        WidgetStyle.COMPACT -> 2.2f
        WidgetStyle.STACKED_DATE -> 1f
        WidgetStyle.DIAL_INDEX, WidgetStyle.DIAL_MINIMAL, WidgetStyle.DIAL_DOTS -> 1f
        WidgetStyle.DAY_CARD -> 1f
        WidgetStyle.DATE_LINE -> 2.6f
        WidgetStyle.MONTH -> 1.05f
    }

    /** Compact one-line form for storage: key=value pairs. Unknown or missing keys fall back to defaults. */
    fun encode(): String = listOf(
        "v" to "1", "style" to style.name, "font" to typeface, "text" to hex(textColor), "accent" to hex(accentColor),
        "twoTone" to twoTone.toString(), "ornament" to ornament.name, "bg" to background.name, "bgColor" to hex(backgroundColor),
        "opacity" to opacity.toString(), "corner" to corner.name, "border" to border.toString(), "align" to align.name,
        "time" to timeFormat.name, "date" to dateStyle.name, "face" to dialFace.toString(), "seconds" to seconds.toString(),
    ).joinToString("|") { (k, v) -> "$k=$v" }

    companion object {
        private fun hex(argb: Long) = "%08X".format(argb and 0xFFFFFFFFL)

        /** Defaults per family. Digital and Clock and date match the original Celestial widgets. */
        fun default(type: WidgetType): WidgetSpec = when (type) {
            WidgetType.DIGITAL -> WidgetSpec(WidgetStyle.ORNAMENT, "cormorant_light", 0xFFDCEBFA, 0xFFDCEBFA)
            WidgetType.CLOCK_DATE -> WidgetSpec(WidgetStyle.ORNAMENT_DATE, "cormorant_light", 0xFFDCEBFA, 0xFFDCEBFA)
            WidgetType.ANALOGUE -> WidgetSpec(
                WidgetStyle.DIAL_INDEX, textColor = 0xFFFFFFFF, accentColor = 0xFFFF7A59,
                dialFace = true, backgroundColor = 0xFF141418, opacity = 55,
            )
            WidgetType.DATE -> WidgetSpec(
                WidgetStyle.DAY_CARD, "inter_thin", 0xFFFFFFFF, 0xFFFF6B5A,
                background = WidgetBackground.GLASS, opacity = 18, border = true,
            )
        }

        fun decode(s: String?, fallback: WidgetType): WidgetSpec {
            val d = default(fallback)
            if (s.isNullOrBlank()) return d
            val m = s.split("|").mapNotNull { it.split("=", limit = 2).takeIf { p -> p.size == 2 }?.let { p -> p[0] to p[1] } }.toMap()
            fun color(k: String, def: Long) = m[k]?.toLongOrNull(16)?.let { it or 0xFF000000L } ?: def
            fun bool(k: String, def: Boolean) = m[k]?.toBooleanStrictOrNull() ?: def
            val style = m["style"]?.let { n -> WidgetStyle.entries.firstOrNull { it.name == n } }?.takeIf { it.type == fallback } ?: d.style
            return WidgetSpec(
                style = style,
                typeface = m["font"]?.takeIf { WidgetTypefaces.byId(it) != null } ?: d.typeface,
                textColor = color("text", d.textColor),
                accentColor = color("accent", d.accentColor),
                twoTone = bool("twoTone", d.twoTone),
                ornament = m["ornament"]?.let { n -> WidgetOrnament.entries.firstOrNull { it.name == n } } ?: d.ornament,
                background = m["bg"]?.let { n -> WidgetBackground.entries.firstOrNull { it.name == n } } ?: d.background,
                backgroundColor = color("bgColor", d.backgroundColor),
                opacity = m["opacity"]?.toIntOrNull()?.coerceIn(0, 100) ?: d.opacity,
                corner = m["corner"]?.let { n -> WidgetCorner.entries.firstOrNull { it.name == n } } ?: d.corner,
                border = bool("border", d.border),
                align = m["align"]?.let { n -> WidgetAlign.entries.firstOrNull { it.name == n } } ?: d.align,
                timeFormat = m["time"]?.let { n -> TimeFormat.entries.firstOrNull { it.name == n } } ?: d.timeFormat,
                dateStyle = m["date"]?.let { n -> DateStyle.entries.firstOrNull { it.name == n } } ?: d.dateStyle,
                dialFace = bool("face", d.dialFace),
                seconds = bool("seconds", d.seconds),
            )
        }
    }
}

/** A typeface the text widgets can use: a bundled font at a fixed weight, each with its own generated layout. */
data class WidgetTypeface(val id: String, val name: String, val fontId: String, val weight: Int, val layoutName: String = "widget_text_$id")

object WidgetTypefaces {
    const val DEFAULT = "inter_thin"

    /** Keep in step with tools/widgets/generate_layouts.py. */
    val all = listOf(
        WidgetTypeface("inter_thin", "Inter Thin", "inter", 200),
        WidgetTypeface("inter_bold", "Inter Bold", "inter", 700),
        WidgetTypeface("outfit_light", "Outfit Light", "outfit", 250),
        WidgetTypeface("outfit_black", "Outfit Black", "outfit", 850),
        WidgetTypeface("space_grotesk", "Space Grotesk", "space_grotesk", 500),
        WidgetTypeface("syne_bold", "Syne", "syne", 700),
        WidgetTypeface("josefin_light", "Josefin", "josefin_sans", 300),
        WidgetTypeface("cormorant_light", "Cormorant", "cormorant", 300),
        WidgetTypeface("playfair", "Playfair", "playfair", 500),
        WidgetTypeface("roboto_mono", "Roboto Mono", "roboto_mono", 300),
    )

    fun byId(id: String): WidgetTypeface? = all.firstOrNull { it.id == id }
    fun of(id: String): WidgetTypeface = byId(id) ?: all.first()
}
