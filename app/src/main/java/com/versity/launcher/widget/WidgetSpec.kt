package com.versity.launcher.widget

/**
 * The widget families offered in the widget studio and the system widget picker. The last four are the
 * collection: a live countdown, a radial app launcher, a circular app shortcut and a decorative badge.
 */
enum class WidgetType(val title: String, val description: String, val collection: Boolean = false) {
    DIGITAL("Digital clock", "One line, stacked or with an ornament"),
    CLOCK_DATE("Clock and date", "Time with the date, four layouts"),
    ANALOGUE("Analogue clock", "Twelve dials, six hand styles"),
    DATE("Date and calendar", "Day card, date line or month"),
    COUNTDOWN("Countdown", "Live days, hours, minutes and seconds", collection = true),
    LAUNCHER("App launcher", "Five apps on a radial shape", collection = true),
    SHORTCUT("App shortcut", "One app in layered rings", collection = true),
    BADGE("Badge", "A scalloped badge with your logo", collection = true),
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
    DIAL_BARS(WidgetType.ANALOGUE, "Bars"),
    DIAL_QUARTERS(WidgetType.ANALOGUE, "Quarters"),
    DIAL_NUMBERS(WidgetType.ANALOGUE, "Numbers"),
    DIAL_ROMAN(WidgetType.ANALOGUE, "Roman"),
    DIAL_RAILWAY(WidgetType.ANALOGUE, "Railway"),
    DIAL_SECTOR(WidgetType.ANALOGUE, "Sector"),
    DIAL_DIVER(WidgetType.ANALOGUE, "Diver"),
    DIAL_SEGMENTS(WidgetType.ANALOGUE, "Segments"),
    DIAL_DUAL(WidgetType.ANALOGUE, "Dual"),
    DAY_CARD(WidgetType.DATE, "Day card"),
    DATE_LINE(WidgetType.DATE, "Date line"),
    MONTH(WidgetType.DATE, "Month"),
    COUNTDOWN_CARD(WidgetType.COUNTDOWN, "Card"),
    COUNTDOWN_BIG(WidgetType.COUNTDOWN, "Big days"),
    RADIAL_FLOWER(WidgetType.LAUNCHER, "Flower"),
    RADIAL_RINGS(WidgetType.LAUNCHER, "Rings"),
    RADIAL_ORBIT(WidgetType.LAUNCHER, "Orbit"),
    RADIAL_CROSS(WidgetType.LAUNCHER, "Cross"),
    RADIAL_TILE(WidgetType.LAUNCHER, "Tile"),
    SHORTCUT_RINGS(WidgetType.SHORTCUT, "Rings"),
    SHORTCUT_HALO(WidgetType.SHORTCUT, "Halo"),
    BADGE_SCALLOP(WidgetType.BADGE, "Scalloped"),
    BADGE_PETALS(WidgetType.BADGE, "Petals"),
    BADGE_CIRCLE(WidgetType.BADGE, "Circle"),
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

/** Analogue hands. */
enum class HandStyle(val label: String) {
    CLASSIC("Classic"), SLIM("Slim"), BOLD("Bold"), NEEDLE("Needle"), DAUPHINE("Dauphine"), SKELETON("Skeleton"),
}

/** Analogue face outline. */
enum class FaceShape(val label: String) { ROUND("Round"), SQUARE("Rounded square") }

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
    val hands: HandStyle = HandStyle.CLASSIC,
    val faceShape: FaceShape = FaceShape.ROUND,
    /** Countdown: the event's name, shown in capitals. */
    val title: String = "",
    /** Countdown: the moment counted down to, in epoch milliseconds; 0 means the next New Year. */
    val target: Long = 0L,
    /** Launcher (five: top, left, centre, right, bottom), shortcut and badge: apps by [com.versity.launcher.data.AppEntry.key]. */
    val apps: List<String> = emptyList(),
    /** Badge: the user's own logo or picture (a private copy), shown instead of an app icon. */
    val image: String? = null,
    /** Icon size as a share of its circle, 40..100. */
    val iconScale: Int = 64,
    /** Space between the launcher's circles, 0..30 (hundredths of a circle). */
    val spacing: Int = 6,
    /** Ring thickness, 1..10 (hundredths of a circle). */
    val ringWidth: Int = 4,
    /** Number of concentric rings, 0..3 (the badge always has at least one). */
    val rings: Int = 2,
    /** A soft shadow under rings and shapes. */
    val shadow: Boolean = true,
    /** Launcher and shortcut: app icons cut to circles, so every app matches. */
    val roundIcons: Boolean = false,
    /** Launcher: the centre app a little larger, with brighter rings. */
    val featured: Boolean = false,
) {
    val type: WidgetType get() = style.type

    /** Whether the style shows the date, so the studio can hide date options that do nothing. */
    val showsDate: Boolean get() = style in setOf(
        WidgetStyle.ORNAMENT_DATE, WidgetStyle.EDITORIAL, WidgetStyle.COMPACT, WidgetStyle.STACKED_DATE, WidgetStyle.DATE_LINE,
    )
    val usesOrnament: Boolean get() = style == WidgetStyle.ORNAMENT || style == WidgetStyle.ORNAMENT_DATE
    val showsTime: Boolean get() = type == WidgetType.DIGITAL || type == WidgetType.CLOCK_DATE
    /** Analogue dials with numerals use a typeface; the others are marks only. */
    val hasNumerals: Boolean get() = style in setOf(WidgetStyle.DIAL_QUARTERS, WidgetStyle.DIAL_NUMBERS, WidgetStyle.DIAL_ROMAN, WidgetStyle.DIAL_DUAL)
    val usesTypeface: Boolean get() = when (type) {
        WidgetType.DIGITAL, WidgetType.CLOCK_DATE, WidgetType.DATE -> true
        WidgetType.ANALOGUE -> hasNumerals
        else -> false
    }

    /** Width over height that suits this style; used for previews and widgets on Versity's home screen. */
    val aspect: Float get() = when (style) {
        WidgetStyle.LINE -> 2.6f
        WidgetStyle.STACKED -> 1.1f
        WidgetStyle.ORNAMENT -> 2.2f
        WidgetStyle.ORNAMENT_DATE, WidgetStyle.EDITORIAL -> 1.9f
        WidgetStyle.COMPACT -> 2.2f
        WidgetStyle.STACKED_DATE -> 1f
        WidgetStyle.DAY_CARD -> 1f
        WidgetStyle.DATE_LINE -> 2.6f
        WidgetStyle.MONTH -> 1.05f
        WidgetStyle.COUNTDOWN_CARD -> 2f
        WidgetStyle.COUNTDOWN_BIG -> 1.5f
        // Dials, the launcher, the shortcut and the badge are square.
        else -> 1f
    }

    /** Compact one-line form for storage: key=value pairs. Unknown or missing keys fall back to defaults. */
    fun encode(): String = listOf(
        "v" to "1", "style" to style.name, "font" to typeface, "text" to hex(textColor), "accent" to hex(accentColor),
        "twoTone" to twoTone.toString(), "ornament" to ornament.name, "bg" to background.name, "bgColor" to hex(backgroundColor),
        "opacity" to opacity.toString(), "corner" to corner.name, "border" to border.toString(), "align" to align.name,
        "time" to timeFormat.name, "date" to dateStyle.name, "face" to dialFace.toString(), "seconds" to seconds.toString(),
        "hands" to hands.name, "faceShape" to faceShape.name,
        "title" to esc(title), "target" to target.toString(), "apps" to apps.joinToString(",") { esc(it) },
        "image" to esc(image.orEmpty()), "iconScale" to iconScale.toString(), "spacing" to spacing.toString(),
        "ringWidth" to ringWidth.toString(), "rings" to rings.toString(), "shadow" to shadow.toString(),
        "roundIcons" to roundIcons.toString(), "featured" to featured.toString(),
    ).joinToString("|") { (k, v) -> "$k=$v" }

    companion object {
        private fun hex(argb: Long) = "%08X".format(argb and 0xFFFFFFFFL)
        /** Free text and URIs are percent-encoded so "|", "=" and "," never break the format. */
        private fun esc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
        private fun unesc(s: String) = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrDefault("")

        /** Emerald on near-black, as in the collection's reference design. */
        const val EMERALD = 0xFF7DD69BL
        const val DEEP_GREEN = 0xFF0E1913L

        /** Defaults per family. Digital and Clock and date match the original Celestial widgets. */
        fun default(type: WidgetType): WidgetSpec = when (type) {
            WidgetType.DIGITAL -> WidgetSpec(WidgetStyle.ORNAMENT, "cormorant_light", 0xFFDCEBFA, 0xFFDCEBFA)
            WidgetType.CLOCK_DATE -> WidgetSpec(WidgetStyle.ORNAMENT_DATE, "cormorant_light", 0xFFDCEBFA, 0xFFDCEBFA)
            WidgetType.ANALOGUE -> WidgetSpec(
                WidgetStyle.DIAL_INDEX, "inter_bold", textColor = 0xFFFFFFFF, accentColor = 0xFFFF7A59,
                dialFace = true, backgroundColor = 0xFF141418, opacity = 55,
            )
            WidgetType.DATE -> WidgetSpec(
                WidgetStyle.DAY_CARD, "inter_thin", 0xFFFFFFFF, 0xFFFF6B5A,
                background = WidgetBackground.GLASS, opacity = 18, border = true,
            )
            WidgetType.COUNTDOWN -> WidgetSpec(
                WidgetStyle.COUNTDOWN_CARD, textColor = 0xFFE8EEE9, accentColor = EMERALD,
                background = WidgetBackground.SOLID, backgroundColor = DEEP_GREEN, opacity = 94, corner = WidgetCorner.LARGE,
                border = true, title = "New Year",
            )
            WidgetType.LAUNCHER -> WidgetSpec(
                WidgetStyle.RADIAL_FLOWER, textColor = 0xFF3A3A40, accentColor = 0xFF1C1C20,
                background = WidgetBackground.SOLID, backgroundColor = 0xFF060607, opacity = 100, iconScale = 42, ringWidth = 5, rings = 2,
            )
            WidgetType.SHORTCUT -> WidgetSpec(
                WidgetStyle.SHORTCUT_RINGS, textColor = 0xFF3A3A40, accentColor = 0xFF1C1C20,
                background = WidgetBackground.SOLID, backgroundColor = 0xFF060607, opacity = 100, iconScale = 48, ringWidth = 5, rings = 2,
            )
            WidgetType.BADGE -> WidgetSpec(
                WidgetStyle.BADGE_SCALLOP, textColor = 0xFFFFFFFF, accentColor = 0xFF3A5546,
                backgroundColor = 0xFF263D31, opacity = 100, iconScale = 62, ringWidth = 3, rings = 1,
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
                hands = m["hands"]?.let { n -> HandStyle.entries.firstOrNull { it.name == n } } ?: d.hands,
                faceShape = m["faceShape"]?.let { n -> FaceShape.entries.firstOrNull { it.name == n } } ?: d.faceShape,
                title = m["title"]?.let(::unesc) ?: d.title,
                target = m["target"]?.toLongOrNull() ?: d.target,
                apps = m["apps"]?.split(",")?.map(::unesc)?.filter { it.isNotBlank() } ?: d.apps,
                image = m["image"]?.let(::unesc)?.ifBlank { null } ?: d.image,
                iconScale = m["iconScale"]?.toIntOrNull()?.coerceIn(40, 100) ?: d.iconScale,
                spacing = m["spacing"]?.toIntOrNull()?.coerceIn(0, 30) ?: d.spacing,
                ringWidth = m["ringWidth"]?.toIntOrNull()?.coerceIn(1, 10) ?: d.ringWidth,
                rings = m["rings"]?.toIntOrNull()?.coerceIn(0, 3) ?: d.rings,
                shadow = bool("shadow", d.shadow),
                roundIcons = bool("roundIcons", d.roundIcons),
                featured = bool("featured", d.featured),
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
