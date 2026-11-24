package com.versity.launcher.data

enum class ClockStyle { STACKED, SINGLE_LINE, ANALOGUE }

/** The decorative drawing behind everything: home, lock-style screen, drawer, previews and the exported lock wallpaper. */
enum class Backdrop { NONE, ORBIT, RINGS, HORIZON, WAVES }

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
) {
    companion object {
        const val MIN_DIM = 0
        const val MAX_DIM = 80
        const val MAX_FAVOURITES = 8
        val DEFAULT = LauncherSettings()
    }
}
