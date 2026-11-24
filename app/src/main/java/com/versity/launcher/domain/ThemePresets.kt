package com.versity.launcher.domain

import androidx.annotation.FontRes
import com.versity.launcher.R
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout

/** All colours are opaque ARGB longs (0xFFRRGGBB). */
data class Palette(
    val id: String,
    val name: String,
    val backgroundTop: Long,
    val backgroundBottom: Long,
    val surface: Long,
    val onSurface: Long,
    val onSurfaceMuted: Long,
    val accent: Long,
    /** Rim colours of the two large circles in the ORBIT layout and the lock-style screen. */
    val rimUpper: Long,
    val rimLower: Long,
)

/** A complete look: applying it sets all of these at once. Wallpaper, favourites and lock-style are untouched. */
data class ThemeBundle(
    val id: String,
    val name: String,
    val tagline: String,
    val paletteId: String,
    val fontId: String,
    val clockStyle: ClockStyle,
    val dimPercent: Int,
    val accentArgb: Long?,
    val layout: HomeLayout,
    val backdrop: Backdrop,
)

data class FontOption(val id: String, val name: String, @FontRes val resId: Int)

/** Bundled palettes and fonts. The first entry of each list is the fallback for unknown ids. */
object ThemePresets {
    val palettes: List<Palette> = listOf(
        Palette(
            id = "midnight", name = "Midnight",
            backgroundTop = 0xFF1B1840, backgroundBottom = 0xFF0B0C16,
            surface = 0xFF1A1D2B, onSurface = 0xFFEDEFF7, onSurfaceMuted = 0xFF9EA3BC,
            accent = 0xFF8FA3FF,
            rimUpper = 0xFF9FB4D8, rimLower = 0xFF8F7BE8,
        ),
        Palette(
            id = "ocean", name = "Ocean",
            backgroundTop = 0xFF0E3A52, backgroundBottom = 0xFF08141C,
            surface = 0xFF10222D, onSurface = 0xFFE8F3F8, onSurfaceMuted = 0xFF8FB0C0,
            accent = 0xFF4FC3E8,
            rimUpper = 0xFF8FD3E8, rimLower = 0xFF5B8CFF,
        ),
        Palette(
            id = "ember", name = "Ember",
            backgroundTop = 0xFF3D1A12, backgroundBottom = 0xFF140A08,
            surface = 0xFF241512, onSurface = 0xFFF7ECE8, onSurfaceMuted = 0xFFC0988C,
            accent = 0xFFFF8A5C,
            rimUpper = 0xFFE8B49F, rimLower = 0xFFFF7A59,
        ),
        Palette(
            id = "graphite", name = "Graphite",
            backgroundTop = 0xFF2A2C31, backgroundBottom = 0xFF0D0D0F,
            surface = 0xFF1C1D21, onSurface = 0xFFEDEDEF, onSurfaceMuted = 0xFF9A9CA3,
            accent = 0xFFB8C4D0,
            rimUpper = 0xFFC9CDD4, rimLower = 0xFF8E96A8,
        ),
        Palette(
            id = "nebula", name = "Nebula",
            backgroundTop = 0xFF06070C, backgroundBottom = 0xFF000000,
            surface = 0xFF14121C, onSurface = 0xFFF0F1F5, onSurfaceMuted = 0xFFA6A8B4,
            accent = 0xFFB69CFF,
            rimUpper = 0xFFA9B7CC, rimLower = 0xFFB48CFF,
        ),
    )

    val fonts: List<FontOption> = listOf(
        FontOption("inter", "Inter", R.font.inter),
        FontOption("space_grotesk", "Space Grotesk", R.font.space_grotesk),
        FontOption("outfit", "Outfit", R.font.outfit),
        FontOption("roboto_mono", "Roboto Mono", R.font.roboto_mono),
    )

    val clockStyles: List<ClockStyle> = ClockStyle.entries

    /** Complete looks. Applying one sets palette, font, clock, dim, accent and layout together. */
    val themes: List<ThemeBundle> = listOf(
        ThemeBundle("orbit", "Orbit", "Two circles, big clock, widget row", "nebula", "outfit", ClockStyle.SINGLE_LINE, 20, null, HomeLayout.ORBIT, Backdrop.ORBIT),
        ThemeBundle("aurora", "Aurora", "Stacked clock on deep indigo", "midnight", "space_grotesk", ClockStyle.STACKED, 30, null, HomeLayout.CLASSIC, Backdrop.RINGS),
        ThemeBundle("tide", "Tide", "Calm blues, clean sans", "ocean", "inter", ClockStyle.SINGLE_LINE, 25, null, HomeLayout.CLASSIC, Backdrop.WAVES),
        ThemeBundle("dusk", "Dusk", "Warm tones, analogue face", "ember", "outfit", ClockStyle.ANALOGUE, 35, null, HomeLayout.CLASSIC, Backdrop.HORIZON),
        ThemeBundle("mono", "Mono", "Graphite, monospace, nothing extra", "graphite", "roboto_mono", ClockStyle.SINGLE_LINE, 40, null, HomeLayout.MINIMAL, Backdrop.NONE),
    )

    /** Unknown ids fall back to the first palette so a stale stored id never crashes the UI. */
    fun palette(id: String): Palette = palettes.firstOrNull { it.id == id } ?: palettes.first()

    fun font(id: String): FontOption = fonts.firstOrNull { it.id == id } ?: fonts.first()
}
