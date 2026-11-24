package com.versity.launcher.data

/** The last six are the big-clock family: large faces that fill the width. */
enum class ClockStyle { STACKED, SINGLE_LINE, ANALOGUE, ORNAMENT, GIANT, OUTLINE, NEON, EXTRUDED, SEGMENT, FLIP, STAGE, GLASS }

/**
 * Decoration drawn with the ORNAMENT clock style.
 * CELESTIAL: fading line with a four-point star in the middle. UNDERLINE: fading line only.
 * MOON: fading line with a crescent. CORNERS: thin corner marks framing the time.
 */
enum class Ornament { CELESTIAL, UNDERLINE, MOON, CORNERS }

/** The decorative drawing behind everything: home, lock-style screen, drawer, previews and the exported lock wallpaper. */
enum class Backdrop { NONE, RINGS, HORIZON, WAVES, STARS, AURORA, DUNES, PEAKS, ECLIPSE, BOKEH, GRID, ARCS, RAIN }

/** PLAIN: neutral drawer that follows the system light/dark mode and font. THEMED: drawer uses the theme. */
enum class DrawerStyle { PLAIN, THEMED }

/** Light or dark for the launcher's own screens (Customise, sheets, plain drawer). SYSTEM follows the phone. */
enum class UiMode { SYSTEM, LIGHT, DARK }

/** ORIGINAL: app icons exactly as the apps ship them. THEMED: icons tinted to the theme's accent. */
enum class IconStyle { ORIGINAL, THEMED }

/** CLASSIC: clock, widgets, dock. MINIMAL: clock, favourites, dock. ORBIT (shown as "Centered"): centred date and clock, widget row. */
enum class HomeLayout { CLASSIC, MINIMAL, ORBIT }

/**
 * Where the user placed the clock: centre point as fractions of the screen (0..1) and a size factor
 * (1 = the default width). Stored as "x,y,scale" so one preference holds it.
 */
data class ClockPlacement(val x: Float, val y: Float, val scale: Float) {
    fun clamped() = ClockPlacement(x.coerceIn(0.05f, 0.95f), y.coerceIn(0.05f, 0.95f), scale.coerceIn(MIN_SCALE, MAX_SCALE))
    fun encode() = "$x,$y,$scale"

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 1.6f
        val DEFAULT = ClockPlacement(0.5f, 0.24f, 1f)
        fun decode(s: String?): ClockPlacement? = s?.split(",")?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 3 && it.all { v -> v.isFinite() } }?.let { ClockPlacement(it[0], it[1], it[2]).clamped() }
    }
}

/** Wallpapers that ship with the app, addressed by stable resource name so stored choices survive updates. */
object BundledWallpaper {
    const val PREFIX = "android.resource://com.versity.launcher/drawable/wp_"
    fun uri(id: String) = PREFIX + id
    fun idOf(uri: String?): String? = uri?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)
}

data class LauncherSettings(
    /** Photo or bundled wallpaper; null shows the palette gradient and backdrop. */
    val wallpaperUri: String? = BundledWallpaper.uri("leviathan"),
    val dimPercent: Int = 10,           // 0..80
    val clockStyle: ClockStyle = ClockStyle.ORNAMENT,
    val fontId: String = "cormorant",   // key into ThemePresets.fonts
    val paletteId: String = "abyss",    // key into ThemePresets.palettes
    val accentArgb: Long? = null,       // null = use palette accent
    val layout: HomeLayout = HomeLayout.MINIMAL,
    val backdrop: Backdrop = Backdrop.NONE,
    val favourites: List<String> = emptyList(), // AppEntry.key values, in display order
    val lockStyleEnabled: Boolean = false,
    val ornament: Ornament = Ornament.CELESTIAL,
    /** ORNAMENT style only: show the date under the divider (and hide the separate date line). */
    val ornamentDate: Boolean = false,
    /** Theme used on the lock screen; null means the lock screen matches the home screen. */
    val lockThemeId: String? = null,
    /** Wallpaper used only on the lock screen; null uses the lock theme's or the home wallpaper. */
    val lockWallpaperUri: String? = null,
    val drawerStyle: DrawerStyle = DrawerStyle.PLAIN,
    val iconStyle: IconStyle = IconStyle.ORIGINAL,
    val uiMode: UiMode = UiMode.SYSTEM,
    /** Free placement of the home clock; null uses the layout's own position. */
    val clockPlacement: ClockPlacement? = null,
    /** Free placement of the lock-screen clock; null uses the layout's own position. */
    val lockClockPlacement: ClockPlacement? = null,
) {
    companion object {
        const val MIN_DIM = 0
        const val MAX_DIM = 80
        const val MAX_FAVOURITES = 8
        val DEFAULT = LauncherSettings()
    }
}
