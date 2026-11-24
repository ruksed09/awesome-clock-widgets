package com.versity.launcher.data

enum class ClockStyle { STACKED, SINGLE_LINE, ANALOGUE, ORNAMENT }

/**
 * Decoration drawn with the ORNAMENT clock style.
 * CELESTIAL: fading line with a four-point star in the middle. UNDERLINE: fading line only.
 * MOON: fading line with a crescent. CORNERS: thin corner marks framing the time.
 */
enum class Ornament { CELESTIAL, UNDERLINE, MOON, CORNERS }

/** The decorative drawing behind everything: home, lock-style screen, drawer, previews and the exported lock wallpaper. */
enum class Backdrop { NONE, ORBIT, RINGS, HORIZON, WAVES, STARS, AURORA, DUNES, PEAKS, ECLIPSE, BOKEH, GRID, ARCS, RAIN }

/** PLAIN: neutral drawer that follows the system light/dark mode and font. THEMED: drawer uses the theme. */
enum class DrawerStyle { PLAIN, THEMED }

/** ORIGINAL: app icons exactly as the apps ship them. THEMED: icons tinted to the theme's accent. */
enum class IconStyle { ORIGINAL, THEMED }

/** CLASSIC: clock, widgets, dock. MINIMAL: clock, favourites, dock. ORBIT: centred date and clock, widget row, two large circles. */
enum class HomeLayout { CLASSIC, MINIMAL, ORBIT }

data class LauncherSettings(
    val wallpaperUri: String? = null,   // null = default gradient
    val dimPercent: Int = 20,           // 0..80
    val clockStyle: ClockStyle = ClockStyle.SINGLE_LINE,
    val fontId: String = "outfit",      // key into ThemePresets.fonts
    val paletteId: String = "nebula",   // key into ThemePresets.palettes
    val accentArgb: Long? = null,       // null = use palette accent
    val layout: HomeLayout = HomeLayout.ORBIT,
    val backdrop: Backdrop = Backdrop.ORBIT,
    val favourites: List<String> = emptyList(), // AppEntry.key values, in display order
    val lockStyleEnabled: Boolean = false,
    val ornament: Ornament = Ornament.CELESTIAL,
    /** ORNAMENT style only: show the date under the divider (and hide the separate date line). */
    val ornamentDate: Boolean = false,
    /** Theme used on the lock screen; null means the lock screen matches the home screen. */
    val lockThemeId: String? = null,
    val drawerStyle: DrawerStyle = DrawerStyle.PLAIN,
    val iconStyle: IconStyle = IconStyle.ORIGINAL,
) {
    companion object {
        const val MIN_DIM = 0
        const val MAX_DIM = 80
        const val MAX_FAVOURITES = 8
        val DEFAULT = LauncherSettings()
    }
}
