package com.versity.launcher.domain

import androidx.annotation.FontRes
import com.versity.launcher.R
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.BundledWallpaper
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.Ornament

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
    /** Rim colours of the two large circles in the ORBIT layout and the lock-style screen; also the backdrop tints. */
    val rimUpper: Long,
    val rimLower: Long,
)

/** Gallery filter groups. */
enum class ThemeCategory(val label: String) {
    NIGHT("Night sky"),
    NATURE("Nature"),
    MINIMAL("Minimal"),
    BOLD("Bold"),
    ELEGANT("Elegant"),
    GLASS("Glass and clay"),
    BIG("Big clock"),
}

/** A complete look: applying it sets all of these at once. Wallpaper, favourites, drawer and icons are untouched. */
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
    /** Ornament to apply with the theme; null leaves the current one. */
    val ornament: Ornament? = null,
    /** Whether an ornament clock shows the date; null leaves the current choice. */
    val ornamentDate: Boolean? = null,
    val category: ThemeCategory = ThemeCategory.MINIMAL,
    /** Bundled wallpaper the theme brings (see [Wallpapers]); null uses the palette gradient and backdrop. */
    val wallpaperId: String? = null,
) {
    val wallpaperUri: String? get() = wallpaperId?.let(BundledWallpaper::uri)
}

data class FontOption(val id: String, val name: String, @FontRes val resId: Int)

/** Bundled palettes, fonts and themes. The first entry of each list is the fallback for unknown ids. */
object ThemePresets {
    val palettes: List<Palette> = listOf(
        Palette("midnight", "Midnight", 0xFF1B1840, 0xFF0B0C16, 0xFF1A1D2B, 0xFFEDEFF7, 0xFF9EA3BC, 0xFF8FA3FF, 0xFF9FB4D8, 0xFF8F7BE8),
        Palette("ocean", "Ocean", 0xFF0E3A52, 0xFF08141C, 0xFF10222D, 0xFFE8F3F8, 0xFF8FB0C0, 0xFF4FC3E8, 0xFF8FD3E8, 0xFF5B8CFF),
        Palette("ember", "Ember", 0xFF3D1A12, 0xFF140A08, 0xFF241512, 0xFFF7ECE8, 0xFFC0988C, 0xFFFF8A5C, 0xFFE8B49F, 0xFFFF7A59),
        Palette("graphite", "Graphite", 0xFF2A2C31, 0xFF0D0D0F, 0xFF1C1D21, 0xFFEDEDEF, 0xFF9A9CA3, 0xFFB8C4D0, 0xFFC9CDD4, 0xFF8E96A8),
        Palette("nebula", "Nebula", 0xFF06070C, 0xFF000000, 0xFF14121C, 0xFFF0F1F5, 0xFFA6A8B4, 0xFFB69CFF, 0xFFA9B7CC, 0xFFB48CFF),
        Palette("abyss", "Abyss", 0xFF1B3F66, 0xFF06142A, 0xFF0E2238, 0xFFDCEBFA, 0xFF9DB6CF, 0xFFBFE0FF, 0xFFCFE6FF, 0xFF7FA8D6),
        Palette("moonstone", "Moonstone", 0xFF2A3140, 0xFF0C0F15, 0xFF181C25, 0xFFE6EAF2, 0xFFA3ABBD, 0xFFD8E0F0, 0xFFE0E6F2, 0xFF8C96AC),
        Palette("boreal", "Boreal", 0xFF0D2E33, 0xFF03090D, 0xFF0E1F22, 0xFFE4F5F2, 0xFF92B8B2, 0xFF6EF2C2, 0xFF6EF2C2, 0xFF8A7BFF),
        Palette("violet", "Violet", 0xFF2B1846, 0xFF0B0614, 0xFF1B1229, 0xFFF1EAFB, 0xFFB5A6CC, 0xFFD3B4FF, 0xFFE2CCFF, 0xFF9B6CFF),
        Palette("sand", "Sand", 0xFF4A3222, 0xFF160E09, 0xFF2A1E16, 0xFFF6EBDD, 0xFFC9AE93, 0xFFF0B77A, 0xFFE8C39B, 0xFFC97E4A),
        Palette("glacier", "Glacier", 0xFF2C4256, 0xFF0A121A, 0xFF16222D, 0xFFEEF6FC, 0xFFA5BCCF, 0xFF9FD8FF, 0xFFDDEFFC, 0xFF6F93B5),
        Palette("fern", "Fern", 0xFF1D3524, 0xFF07100A, 0xFF122017, 0xFFE9F3EA, 0xFFA0BBA5, 0xFFA8E0A0, 0xFFC8F0B8, 0xFF5FA86A),
        Palette("lagoon", "Lagoon", 0xFF0B4A55, 0xFF031418, 0xFF0A2A30, 0xFFE6FBFB, 0xFF94C9CC, 0xFF5EEAD4, 0xFF9EF5EA, 0xFF2BB3C0),
        Palette("rain", "Monsoon", 0xFF2B3740, 0xFF0B1014, 0xFF172026, 0xFFE8EEF2, 0xFFA0AFBA, 0xFF8CC4E8, 0xFFC2D6E4, 0xFF6E8CA3),
        Palette("matcha", "Matcha", 0xFF2E3A2E, 0xFF0F140F, 0xFF1B231B, 0xFFEEF3E6, 0xFFB1BFA6, 0xFFC9DB9A, 0xFFDCE8C0, 0xFF8FA77A),
        Palette("neon", "Neon", 0xFF2A0B40, 0xFF07020F, 0xFF1A0A27, 0xFFF7EEFF, 0xFFC0A6D6, 0xFFFF4FD8, 0xFF3DF5FF, 0xFFFF4FD8),
        Palette("primary", "Primary", 0xFF2B2A28, 0xFF121110, 0xFF1E1D1B, 0xFFF4EFE6, 0xFFB8B0A2, 0xFFF2B33D, 0xFFE5483B, 0xFF3D6FE5),
        Palette("coral", "Coral", 0xFF4A1F2A, 0xFF170A0F, 0xFF2A1219, 0xFFFFEDEA, 0xFFD4A3A6, 0xFFFF8F7A, 0xFFFFB38A, 0xFFE0567A),
        Palette("terminal", "Terminal", 0xFF07140B, 0xFF000000, 0xFF0A140D, 0xFFC8FAD4, 0xFF6FB884, 0xFF3DFF7A, 0xFF3DFF7A, 0xFF1F8F45),
        Palette("noir", "Noir", 0xFF141414, 0xFF000000, 0xFF111111, 0xFFF5F5F5, 0xFF9E9E9E, 0xFFE5484D, 0xFFDDDDDD, 0xFF777777),
        Palette("gilded", "Gilded", 0xFF221B12, 0xFF050403, 0xFF17130D, 0xFFF6EBD3, 0xFFBBA77F, 0xFFE3C07A, 0xFFF2D9A0, 0xFFA98345),
        Palette("rose", "Rose", 0xFF3F1C32, 0xFF12070F, 0xFF26111F, 0xFFFBEAF2, 0xFFCDA2B8, 0xFFF4A6C6, 0xFFFFC7DC, 0xFFB86A9A),
        Palette("mocha", "Mocha", 0xFF3A2A22, 0xFF120C0A, 0xFF231A15, 0xFFF3E9E0, 0xFFBFAA98, 0xFFD9A982, 0xFFE6C7AA, 0xFF9C735A),
        // Light palettes, for light wallpapers: dark ink on pale ground.
        Palette("porcelain", "Porcelain", 0xFFF4F1FB, 0xFFE9E4F5, 0xFFFFFFFF, 0xFF1E1B2E, 0xFF5E5873, 0xFF6A4FD8, 0xFFC9B8FF, 0xFFA9E4F0),
        Palette("sandstone", "Sandstone", 0xFFFFF1E4, 0xFFF0B08E, 0xFFFFF8F1, 0xFF3A1F12, 0xFF6E4A36, 0xFF8E3F22, 0xFFFFE6D2, 0xFFC9775A),
        Palette("clay", "Clay", 0xFFEFEAE4, 0xFFDCD4CB, 0xFFFAF7F3, 0xFF2B241E, 0xFF5E544A, 0xFF9E4F31, 0xFFF3ECE3, 0xFFA9B79B),
    )

    val fonts: List<FontOption> = listOf(
        FontOption("inter", "Inter", R.font.inter),
        FontOption("space_grotesk", "Space Grotesk", R.font.space_grotesk),
        FontOption("outfit", "Outfit", R.font.outfit),
        FontOption("roboto_mono", "Roboto Mono", R.font.roboto_mono),
        FontOption("cormorant", "Cormorant", R.font.cormorant),
        FontOption("playfair", "Playfair", R.font.playfair),
        FontOption("josefin_sans", "Josefin Sans", R.font.josefin_sans),
        FontOption("syne", "Syne", R.font.syne),
    )

    val clockStyles: List<ClockStyle> = ClockStyle.entries

    /** Complete looks. Applying one sets palette, font, clock, dim, accent, layout and backdrop together. */
    val themes: List<ThemeBundle> = listOf(
        // Night sky. Leviathan is the default look.
        ThemeBundle("leviathan", "Leviathan", "Crystal whales in a starlit sea", "abyss", "cormorant", ClockStyle.ORNAMENT, 10, null, HomeLayout.MINIMAL, Backdrop.NONE, Ornament.CELESTIAL, false, ThemeCategory.NIGHT, "leviathan"),
        ThemeBundle("crystal", "Crystal sea", "A drifting lattice of light", "glacier", "inter", ClockStyle.SINGLE_LINE, 10, null, HomeLayout.CLASSIC, Backdrop.NONE, category = ThemeCategory.NIGHT, wallpaperId = "crystal_sea"),
        ThemeBundle("veil", "Aurora veil", "Northern light over a still lake", "boreal", "outfit", ClockStyle.STACKED, 5, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.NIGHT, wallpaperId = "aurora_veil"),
        ThemeBundle("moonrise", "Moonrise", "Moon on the tide, crescent divider", "violet", "cormorant", ClockStyle.ORNAMENT, 5, null, HomeLayout.MINIMAL, Backdrop.NONE, Ornament.MOON, true, ThemeCategory.NIGHT, "moon_tide"),
        ThemeBundle("celestial", "Celestial", "Serif time, starlit line, deep blue", "abyss", "cormorant", ClockStyle.ORNAMENT, 20, null, HomeLayout.MINIMAL, Backdrop.STARS, Ornament.CELESTIAL, false, ThemeCategory.NIGHT),
        ThemeBundle("stardust", "Stardust", "Violet stars, serif time and date", "violet", "playfair", ClockStyle.ORNAMENT, 20, null, HomeLayout.MINIMAL, Backdrop.STARS, Ornament.CELESTIAL, true, ThemeCategory.NIGHT),
        ThemeBundle("lunar", "Lunar", "Pale eclipse, crescent divider", "moonstone", "cormorant", ClockStyle.ORNAMENT, 15, null, HomeLayout.MINIMAL, Backdrop.ECLIPSE, Ornament.MOON, false, ThemeCategory.NIGHT),
        ThemeBundle("northern", "Northern lights", "Green aurora over a dark sky", "boreal", "space_grotesk", ClockStyle.SINGLE_LINE, 15, null, HomeLayout.ORBIT, Backdrop.AURORA, category = ThemeCategory.NIGHT),
        ThemeBundle("nova", "Nova", "Analogue face under a glowing eclipse", "nebula", "space_grotesk", ClockStyle.ANALOGUE, 15, null, HomeLayout.ORBIT, Backdrop.ECLIPSE, category = ThemeCategory.NIGHT),
        // Nature
        ThemeBundle("tide", "Tide", "Calm blues, clean sans", "ocean", "inter", ClockStyle.SINGLE_LINE, 25, null, HomeLayout.CLASSIC, Backdrop.WAVES, category = ThemeCategory.NATURE),
        ThemeBundle("dusk", "Dusk", "Warm tones, analogue face", "ember", "outfit", ClockStyle.ANALOGUE, 35, null, HomeLayout.CLASSIC, Backdrop.HORIZON, category = ThemeCategory.NATURE),
        ThemeBundle("sahara", "Sahara", "Layered dunes, warm sand", "sand", "josefin_sans", ClockStyle.STACKED, 15, null, HomeLayout.CLASSIC, Backdrop.DUNES, category = ThemeCategory.NATURE),
        ThemeBundle("alpine", "Alpine", "Icy peaks, quiet analogue", "glacier", "inter", ClockStyle.ANALOGUE, 15, null, HomeLayout.MINIMAL, Backdrop.PEAKS, category = ThemeCategory.NATURE),
        ThemeBundle("forest", "Forest light", "Soft green bokeh, framed time", "fern", "cormorant", ClockStyle.ORNAMENT, 15, null, HomeLayout.MINIMAL, Backdrop.BOKEH, Ornament.CORNERS, false, ThemeCategory.NATURE),
        ThemeBundle("lagoon", "Lagoon", "Turquoise waves, starlit line", "lagoon", "outfit", ClockStyle.ORNAMENT, 15, null, HomeLayout.ORBIT, Backdrop.WAVES, Ornament.CELESTIAL, false, ThemeCategory.NATURE),
        ThemeBundle("monsoon", "Monsoon", "Slanting rain, cool greys", "rain", "inter", ClockStyle.SINGLE_LINE, 20, null, HomeLayout.MINIMAL, Backdrop.RAIN, category = ThemeCategory.NATURE),
        // Minimal
        ThemeBundle("mono", "Mono", "Graphite, monospace, nothing extra", "graphite", "roboto_mono", ClockStyle.SINGLE_LINE, 40, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.MINIMAL),
        ThemeBundle("aurora", "Indigo", "Stacked clock on deep indigo", "midnight", "space_grotesk", ClockStyle.STACKED, 30, null, HomeLayout.CLASSIC, Backdrop.RINGS, category = ThemeCategory.MINIMAL),
        ThemeBundle("slate", "Slate", "Fine rings, stacked numerals", "graphite", "inter", ClockStyle.STACKED, 30, null, HomeLayout.MINIMAL, Backdrop.RINGS, category = ThemeCategory.MINIMAL),
        ThemeBundle("zen", "Zen", "Sage tones, light geometric type", "matcha", "josefin_sans", ClockStyle.SINGLE_LINE, 20, null, HomeLayout.MINIMAL, Backdrop.RINGS, category = ThemeCategory.MINIMAL),
        ThemeBundle("paper_moon", "Paper moon", "Grey-blue calm, underlined time", "moonstone", "inter", ClockStyle.ORNAMENT, 20, null, HomeLayout.CLASSIC, Backdrop.NONE, Ornament.UNDERLINE, true, ThemeCategory.MINIMAL),
        // Bold
        ThemeBundle("synthwave", "Synthwave", "Neon grid and a retro sun", "neon", "syne", ClockStyle.SINGLE_LINE, 10, null, HomeLayout.ORBIT, Backdrop.GRID, category = ThemeCategory.BOLD),
        ThemeBundle("bauhaus", "Bauhaus", "Primary arcs, geometric type", "primary", "josefin_sans", ClockStyle.STACKED, 10, null, HomeLayout.MINIMAL, Backdrop.ARCS, category = ThemeCategory.BOLD),
        ThemeBundle("sunset", "Sunset", "Coral horizon, high-contrast serif", "coral", "playfair", ClockStyle.SINGLE_LINE, 15, null, HomeLayout.MINIMAL, Backdrop.HORIZON, category = ThemeCategory.BOLD),
        ThemeBundle("terminal", "Terminal", "Green phosphor, monospace", "terminal", "roboto_mono", ClockStyle.STACKED, 0, null, HomeLayout.CLASSIC, Backdrop.GRID, category = ThemeCategory.BOLD),
        ThemeBundle("cyber", "Cyber", "Neon rain, framed monospace", "neon", "roboto_mono", ClockStyle.ORNAMENT, 10, null, HomeLayout.CLASSIC, Backdrop.RAIN, Ornament.CORNERS, false, ThemeCategory.BOLD),
        ThemeBundle("eclipse", "Eclipse", "Black, white and a silver corona", "noir", "syne", ClockStyle.SINGLE_LINE, 0, null, HomeLayout.ORBIT, Backdrop.ECLIPSE, category = ThemeCategory.BOLD),
        // Elegant
        ThemeBundle("noir", "Noir", "Black, white and one fine line", "noir", "playfair", ClockStyle.ORNAMENT, 0, null, HomeLayout.MINIMAL, Backdrop.NONE, Ornament.UNDERLINE, false, ThemeCategory.ELEGANT),
        ThemeBundle("gilded", "Gilded", "Gold corners and arcs on black", "gilded", "cormorant", ClockStyle.ORNAMENT, 0, null, HomeLayout.MINIMAL, Backdrop.ARCS, Ornament.CORNERS, true, ThemeCategory.ELEGANT),
        ThemeBundle("velvet", "Velvet", "Plum and rose, moonlit serif", "rose", "cormorant", ClockStyle.ORNAMENT, 15, null, HomeLayout.CLASSIC, Backdrop.BOKEH, Ornament.MOON, true, ThemeCategory.ELEGANT),
        ThemeBundle("blossom", "Blossom", "Rose bokeh, starlit date", "rose", "josefin_sans", ClockStyle.ORNAMENT, 15, null, HomeLayout.MINIMAL, Backdrop.BOKEH, Ornament.CELESTIAL, true, ThemeCategory.ELEGANT),
        ThemeBundle("mocha", "Mocha", "Coffee tones, classic serif", "mocha", "playfair", ClockStyle.STACKED, 20, null, HomeLayout.CLASSIC, Backdrop.DUNES, category = ThemeCategory.ELEGANT),
        ThemeBundle("satin", "Satin", "Silk folds, underlined serif date", "violet", "josefin_sans", ClockStyle.ORNAMENT, 10, null, HomeLayout.MINIMAL, Backdrop.NONE, Ornament.UNDERLINE, true, ThemeCategory.ELEGANT, "silk"),
        // Glass and clay
        ThemeBundle("bloom", "Glass bloom", "Frosted glass on soft pastels", "porcelain", "outfit", ClockStyle.SINGLE_LINE, 0, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.GLASS, wallpaperId = "glass_bloom"),
        ThemeBundle("prism", "Prism", "Glass shards in neon light", "neon", "space_grotesk", ClockStyle.SINGLE_LINE, 10, null, HomeLayout.ORBIT, Backdrop.NONE, category = ThemeCategory.GLASS, wallpaperId = "prism"),
        ThemeBundle("pebble", "Pebble", "Soft clay stones, framed time", "clay", "inter", ClockStyle.ORNAMENT, 0, null, HomeLayout.CLASSIC, Backdrop.NONE, Ornament.CORNERS, false, ThemeCategory.GLASS, "pebbles"),
        ThemeBundle("dusk_clay", "Dusk clay", "Clay stones after dark", "graphite", "outfit", ClockStyle.ANALOGUE, 0, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.GLASS, wallpaperId = "dusk_clay"),
        // Big clock: large faces over wallpapers
        ThemeBundle("neon_nights", "Neon nights", "Glowing tube digits over glass", "neon", "outfit", ClockStyle.NEON, 15, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "prism"),
        ThemeBundle("mega", "Mega", "Giant warm numerals on sand", "sandstone", "syne", ClockStyle.GIANT, 0, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "dunes"),
        ThemeBundle("hollow", "Hollow", "Outlined numerals over a crystal sea", "glacier", "space_grotesk", ClockStyle.OUTLINE, 5, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "crystal_sea"),
        ThemeBundle("led", "Retro LED", "Seven-segment glow on a neon grid", "terminal", "roboto_mono", ClockStyle.SEGMENT, 0, null, HomeLayout.MINIMAL, Backdrop.GRID, category = ThemeCategory.BIG),
        ThemeBundle("flipboard", "Flipboard", "Split-flap cards on dark clay", "graphite", "inter", ClockStyle.FLIP, 0, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "dusk_clay"),
        ThemeBundle("depth", "Depth", "Extruded 3D numerals on silk", "violet", "outfit", ClockStyle.EXTRUDED, 10, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "silk"),
        ThemeBundle("stage", "Stage", "Date over bold numerals, twin worlds", "boreal", "outfit", ClockStyle.STAGE, 10, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "twin_worlds"),
        ThemeBundle("frost", "Frost", "Frosted glass numerals, supernova", "nebula", "space_grotesk", ClockStyle.GLASS, 10, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.BIG, wallpaperId = "supernova"),
        ThemeBundle("nebula", "Nebula", "Neon tubes through crimson veins", "neon", "josefin_sans", ClockStyle.NEON, 20, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.NIGHT, wallpaperId = "nebula_veins"),
        ThemeBundle("saturnine", "Saturnine", "A ringed giant under giant numerals", "violet", "inter", ClockStyle.GIANT, 10, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.NIGHT, wallpaperId = "ringed_giant"),
        ThemeBundle("sunlit", "Sunlit dunes", "Warm sand under a low sun", "sandstone", "josefin_sans", ClockStyle.STACKED, 0, null, HomeLayout.MINIMAL, Backdrop.NONE, category = ThemeCategory.NATURE, wallpaperId = "dunes"),
    )

    /** Unknown ids fall back to the first palette so a stale stored id never crashes the UI. */
    fun palette(id: String): Palette = palettes.firstOrNull { it.id == id } ?: palettes.first()

    fun font(id: String): FontOption = fonts.firstOrNull { it.id == id } ?: fonts.first()

    fun theme(id: String): ThemeBundle? = themes.firstOrNull { it.id == id }
}

/**
 * These settings with [theme]'s look on top. Favourites, drawer and icon choices are kept. The wallpaper
 * is kept too, unless [useThemeWallpaper] swaps it for the theme's own (or none).
 */
fun LauncherSettings.withTheme(theme: ThemeBundle, useThemeWallpaper: Boolean = false): LauncherSettings = copy(
    wallpaperUri = if (useThemeWallpaper) theme.wallpaperUri else wallpaperUri,
    paletteId = theme.paletteId,
    fontId = theme.fontId,
    clockStyle = theme.clockStyle,
    dimPercent = theme.dimPercent.coerceIn(LauncherSettings.MIN_DIM, LauncherSettings.MAX_DIM),
    accentArgb = theme.accentArgb,
    layout = theme.layout,
    backdrop = theme.backdrop,
    ornament = theme.ornament ?: ornament,
    ornamentDate = theme.ornamentDate ?: ornamentDate,
)

/**
 * The look of the lock screen: the chosen lock theme with its own background, or the home look when
 * the lock screen follows home; a lock-only wallpaper wins over both. A stale theme id falls back to home.
 */
fun LauncherSettings.lockLook(): LauncherSettings {
    val base = (lockThemeId?.let(ThemePresets::theme)?.let { withTheme(it, useThemeWallpaper = true) } ?: this)
        .copy(clockPlacement = lockClockPlacement)
    return lockWallpaperUri?.let { base.copy(wallpaperUri = it) } ?: base
}

/** True for palettes with dark ink on a pale ground, which need dark status bar icons. */
fun Palette.isLight(): Boolean {
    fun lum(argb: Long) = (0.2126 * ((argb shr 16) and 0xFF) + 0.7152 * ((argb shr 8) and 0xFF) + 0.0722 * (argb and 0xFF)) / 255
    return lum(onSurface) < 0.5
}

/** True when [settings] currently shows exactly this theme's look. */
fun ThemeBundle.matches(settings: LauncherSettings): Boolean =
    settings.paletteId == paletteId && settings.fontId == fontId && settings.clockStyle == clockStyle &&
        settings.dimPercent == dimPercent && settings.accentArgb == accentArgb && settings.layout == layout &&
        settings.backdrop == backdrop &&
        (clockStyle != ClockStyle.ORNAMENT || ornament == null || settings.ornament == ornament)
