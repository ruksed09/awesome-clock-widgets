package com.versity.launcher.domain

import androidx.annotation.FontRes
import com.versity.launcher.R
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
    /** Ornament to apply with the theme; null leaves the current one. */
    val ornament: Ornament? = null,
    /** Whether an ornament clock shows the date; null leaves the current choice. */
    val ornamentDate: Boolean? = null,
    val category: ThemeCategory = ThemeCategory.MINIMAL,
    /** Bundled wallpaper the theme brings (see [Wallpapers]); null uses the palette gradient. */
    val wallpaperId: String? = null,
) {
    val wallpaperUri: String? get() = wallpaperId?.let(BundledWallpaper::uri)
}

data class FontOption(val id: String, val name: String, @FontRes val resId: Int)

/** Bundled palettes, fonts and themes. The first entry of each list is the fallback for unknown ids. */
object ThemePresets {
    val palettes: List<Palette> = listOf(
        Palette("midnight", "Midnight", 0xFF1B1840, 0xFF0B0C16, 0xFF1A1D2B, 0xFFEDEFF7, 0xFF9EA3BC, 0xFF8FA3FF),
        Palette("ocean", "Ocean", 0xFF0E3A52, 0xFF08141C, 0xFF10222D, 0xFFE8F3F8, 0xFF8FB0C0, 0xFF4FC3E8),
        Palette("ember", "Ember", 0xFF3D1A12, 0xFF140A08, 0xFF241512, 0xFFF7ECE8, 0xFFC0988C, 0xFFFF8A5C),
        Palette("graphite", "Graphite", 0xFF2A2C31, 0xFF0D0D0F, 0xFF1C1D21, 0xFFEDEDEF, 0xFF9A9CA3, 0xFFB8C4D0),
        Palette("nebula", "Nebula", 0xFF06070C, 0xFF000000, 0xFF14121C, 0xFFF0F1F5, 0xFFA6A8B4, 0xFFB69CFF),
        Palette("abyss", "Abyss", 0xFF1B3F66, 0xFF06142A, 0xFF0E2238, 0xFFDCEBFA, 0xFF9DB6CF, 0xFFBFE0FF),
        Palette("moonstone", "Moonstone", 0xFF2A3140, 0xFF0C0F15, 0xFF181C25, 0xFFE6EAF2, 0xFFA3ABBD, 0xFFD8E0F0),
        Palette("boreal", "Boreal", 0xFF0D2E33, 0xFF03090D, 0xFF0E1F22, 0xFFE4F5F2, 0xFF92B8B2, 0xFF6EF2C2),
        Palette("violet", "Violet", 0xFF2B1846, 0xFF0B0614, 0xFF1B1229, 0xFFF1EAFB, 0xFFB5A6CC, 0xFFD3B4FF),
        Palette("sand", "Sand", 0xFF4A3222, 0xFF160E09, 0xFF2A1E16, 0xFFF6EBDD, 0xFFC9AE93, 0xFFF0B77A),
        Palette("glacier", "Glacier", 0xFF2C4256, 0xFF0A121A, 0xFF16222D, 0xFFEEF6FC, 0xFFA5BCCF, 0xFF9FD8FF),
        Palette("fern", "Fern", 0xFF1D3524, 0xFF07100A, 0xFF122017, 0xFFE9F3EA, 0xFFA0BBA5, 0xFFA8E0A0),
        Palette("lagoon", "Lagoon", 0xFF0B4A55, 0xFF031418, 0xFF0A2A30, 0xFFE6FBFB, 0xFF94C9CC, 0xFF5EEAD4),
        Palette("rain", "Monsoon", 0xFF2B3740, 0xFF0B1014, 0xFF172026, 0xFFE8EEF2, 0xFFA0AFBA, 0xFF8CC4E8),
        Palette("matcha", "Matcha", 0xFF2E3A2E, 0xFF0F140F, 0xFF1B231B, 0xFFEEF3E6, 0xFFB1BFA6, 0xFFC9DB9A),
        Palette("neon", "Neon", 0xFF2A0B40, 0xFF07020F, 0xFF1A0A27, 0xFFF7EEFF, 0xFFC0A6D6, 0xFFFF4FD8),
        Palette("primary", "Primary", 0xFF2B2A28, 0xFF121110, 0xFF1E1D1B, 0xFFF4EFE6, 0xFFB8B0A2, 0xFFF2B33D),
        Palette("coral", "Coral", 0xFF4A1F2A, 0xFF170A0F, 0xFF2A1219, 0xFFFFEDEA, 0xFFD4A3A6, 0xFFFF8F7A),
        Palette("terminal", "Terminal", 0xFF07140B, 0xFF000000, 0xFF0A140D, 0xFFC8FAD4, 0xFF6FB884, 0xFF3DFF7A),
        Palette("noir", "Noir", 0xFF141414, 0xFF000000, 0xFF111111, 0xFFF5F5F5, 0xFF9E9E9E, 0xFFE5484D),
        Palette("gilded", "Gilded", 0xFF221B12, 0xFF050403, 0xFF17130D, 0xFFF6EBD3, 0xFFBBA77F, 0xFFE3C07A),
        Palette("rose", "Rose", 0xFF3F1C32, 0xFF12070F, 0xFF26111F, 0xFFFBEAF2, 0xFFCDA2B8, 0xFFF4A6C6),
        Palette("mocha", "Mocha", 0xFF3A2A22, 0xFF120C0A, 0xFF231A15, 0xFFF3E9E0, 0xFFBFAA98, 0xFFD9A982),
        // Light palettes, for light wallpapers: dark ink on pale ground.
        Palette("porcelain", "Porcelain", 0xFFF4F1FB, 0xFFE9E4F5, 0xFFFFFFFF, 0xFF1E1B2E, 0xFF5E5873, 0xFF6A4FD8),
        Palette("sandstone", "Sandstone", 0xFFFFF1E4, 0xFFF0B08E, 0xFFFFF8F1, 0xFF3A1F12, 0xFF6E4A36, 0xFF8E3F22),
        Palette("clay", "Clay", 0xFFEFEAE4, 0xFFDCD4CB, 0xFFFAF7F3, 0xFF2B241E, 0xFF5E544A, 0xFF9E4F31),
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

    /** Complete looks. Applying one sets palette, font, clock, dim, accent and layout together. */
    val themes: List<ThemeBundle> = listOf(
        // Night sky. Leviathan is the default look.
        t("leviathan", "Leviathan", "Crystal whales in a starlit sea", "abyss", "cormorant", ClockStyle.ORNAMENT, ThemeCategory.NIGHT, "leviathan", 10, Ornament.CELESTIAL, false),
        t("moonrise", "Moonrise", "Moon on the tide, crescent divider", "violet", "cormorant", ClockStyle.ORNAMENT, ThemeCategory.NIGHT, "moon_tide", 5, Ornament.MOON, true),
        t("crystal", "Crystal sea", "Analogue dial over a lattice of light", "glacier", "inter", ClockStyle.ANALOGUE, ThemeCategory.NIGHT, "crystal_sea", 10),
        t("veil", "Aurora veil", "Date over bold numerals, northern light", "boreal", "outfit", ClockStyle.STAGE, ThemeCategory.NIGHT, "aurora_veil", 5),
        t("nebula", "Nebula", "Neon tubes through crimson veins", "neon", "josefin_sans", ClockStyle.NEON, ThemeCategory.NIGHT, "nebula_veins", 20),
        t("saturnine", "Saturnine", "A ringed giant under giant numerals", "violet", "inter", ClockStyle.GIANT, ThemeCategory.NIGHT, "ringed_giant", 10),
        t("stage", "Twin worlds", "Two planets and a bold date", "boreal", "outfit", ClockStyle.STAGE, ThemeCategory.NIGHT, "twin_worlds", 10, layout = HomeLayout.ORBIT),
        t("frost", "Frost", "Frosted glass numerals, supernova", "nebula", "space_grotesk", ClockStyle.GLASS, ThemeCategory.NIGHT, "supernova", 10),
        // Nature
        t("shoreline", "Shoreline", "A calm sea at first light", "clay", "cormorant", ClockStyle.ORNAMENT, ThemeCategory.NATURE, "calm_sea", 0, Ornament.UNDERLINE, false),
        t("alpenglow", "Alpenglow", "Misty peaks at blue hour", "moonstone", "inter", ClockStyle.STAGE, ThemeCategory.NATURE, "misty_peaks", 5),
        t("sunlit", "Sunlit dunes", "Warm sand, quiet serif and date", "sandstone", "playfair", ClockStyle.ORNAMENT, ThemeCategory.NATURE, "dunes", 0, Ornament.UNDERLINE, true),
        t("mega", "Mega", "Giant warm numerals on sand", "sandstone", "syne", ClockStyle.GIANT, ThemeCategory.NATURE, "dunes", 0),
        t("tidewater", "Tidewater", "Analogue dial over a calm sea", "clay", "inter", ClockStyle.ANALOGUE, ThemeCategory.NATURE, "calm_sea", 0, layout = HomeLayout.ORBIT),
        t("lunar", "Lunar", "Moonlit tide, outlined numerals", "moonstone", "space_grotesk", ClockStyle.OUTLINE, ThemeCategory.NATURE, "moon_tide", 10),
        // Minimal
        t("noir", "Noir", "Black, white and one fine line", "noir", "playfair", ClockStyle.ORNAMENT, ThemeCategory.MINIMAL, null, 0, Ornament.UNDERLINE, false),
        t("mono", "Mono", "Graphite, monospace, nothing extra", "graphite", "roboto_mono", ClockStyle.ORNAMENT, ThemeCategory.MINIMAL, null, 0, Ornament.UNDERLINE, true),
        t("paper", "Paper", "Warm off-white and giant ink numerals", "clay", "inter", ClockStyle.GIANT, ThemeCategory.MINIMAL, null, 0),
        t("slate", "Slate", "A graphite dial, nothing else", "graphite", "outfit", ClockStyle.ANALOGUE, ThemeCategory.MINIMAL, null, 0, layout = HomeLayout.ORBIT),
        t("halo", "Halo", "True black and one ring of light", "nebula", "inter", ClockStyle.OUTLINE, ThemeCategory.MINIMAL, "oled_ring", 0),
        t("topo", "Topo", "Contour lines, quiet underline", "boreal", "space_grotesk", ClockStyle.ORNAMENT, ThemeCategory.MINIMAL, "contour", 0, Ornament.UNDERLINE, true),
        t("dawn", "Dawn", "Soft mesh and a starlit date", "porcelain", "josefin_sans", ClockStyle.ORNAMENT, ThemeCategory.MINIMAL, "mesh_dawn", 0, Ornament.CELESTIAL, true),
        // Bold
        t("composition", "Composition", "Bauhaus shapes, giant numerals", "sandstone", "josefin_sans", ClockStyle.GIANT, ThemeCategory.BOLD, "bauhaus", 0),
        t("ridgeline", "Pulse", "Pulse lines and neon tubes", "neon", "outfit", ClockStyle.NEON, ThemeCategory.BOLD, "waveform", 0),
        t("led", "Retro LED", "Seven-segment glow on black", "terminal", "roboto_mono", ClockStyle.SEGMENT, ThemeCategory.BOLD, null, 0),
        t("neon_nights", "Neon nights", "Glowing tube digits over glass", "neon", "outfit", ClockStyle.NEON, ThemeCategory.BOLD, "prism", 15),
        t("prism", "Prism", "Glass shards and 3D numerals", "neon", "space_grotesk", ClockStyle.EXTRUDED, ThemeCategory.BOLD, "prism", 10),
        // Elegant
        t("satin", "Satin", "Silk folds, underlined serif date", "violet", "josefin_sans", ClockStyle.ORNAMENT, ThemeCategory.ELEGANT, "silk", 10, Ornament.UNDERLINE, true),
        t("ink", "Ink", "Deep mesh, moonlit serif", "mocha", "playfair", ClockStyle.ORNAMENT, ThemeCategory.ELEGANT, "mesh_ink", 0, Ornament.MOON, false),
        t("gilded", "Gilded", "Gold corners on black", "gilded", "cormorant", ClockStyle.ORNAMENT, ThemeCategory.ELEGANT, null, 0, Ornament.CORNERS, true),
        t("papercut", "Papercut", "Layered paper at sundown", "sandstone", "playfair", ClockStyle.STAGE, ThemeCategory.ELEGANT, "paper_layers", 0),
        t("depth", "Depth", "Extruded 3D numerals on silk", "violet", "outfit", ClockStyle.EXTRUDED, ThemeCategory.ELEGANT, "silk", 10),
        // Glass and material
        t("bloom", "Glass bloom", "Soft pastels and a bold date", "porcelain", "outfit", ClockStyle.STAGE, ThemeCategory.GLASS, "glass_bloom", 0),
        t("holo", "Holo", "Iridescent foil, giant numerals", "porcelain", "outfit", ClockStyle.GIANT, ThemeCategory.GLASS, "iridescent", 0),
        t("orbs", "Orbs", "Glass spheres, glass numerals", "violet", "space_grotesk", ClockStyle.GLASS, ThemeCategory.GLASS, "glass_orbs", 5),
        t("chrome", "Chrome", "Molten metal, frosted numerals", "graphite", "syne", ClockStyle.GLASS, ThemeCategory.GLASS, "liquid_chrome", 0),
        t("pebble", "Pebble", "Soft clay stones, framed time", "clay", "inter", ClockStyle.ORNAMENT, ThemeCategory.GLASS, "pebbles", 0, Ornament.CORNERS, false),
        t("dusk_clay", "Dusk clay", "Clay stones after dark, analogue dial", "graphite", "outfit", ClockStyle.ANALOGUE, ThemeCategory.GLASS, "dusk_clay", 0),
        // Big clock
        t("hollow", "Hollow", "Outlined numerals over a crystal sea", "glacier", "space_grotesk", ClockStyle.OUTLINE, ThemeCategory.BIG, "crystal_sea", 5),
        t("flipboard", "Flipboard", "Split-flap cards on dark clay", "graphite", "inter", ClockStyle.FLIP, ThemeCategory.BIG, "dusk_clay", 0),
        t("ledger", "Ledger", "Split-flap cards on deep mesh", "mocha", "inter", ClockStyle.FLIP, ThemeCategory.BIG, "mesh_ink", 0),
        t("cosmos", "Cosmos", "LED digits under a supernova", "nebula", "roboto_mono", ClockStyle.SEGMENT, ThemeCategory.BIG, "supernova", 15),
    )

    private fun t(
        id: String, name: String, tagline: String, palette: String, font: String, clock: ClockStyle,
        category: ThemeCategory, wallpaper: String?, dim: Int, ornament: Ornament? = null, ornamentDate: Boolean? = null,
        layout: HomeLayout = HomeLayout.MINIMAL,
    ) = ThemeBundle(id, name, tagline, palette, font, clock, dim, null, layout, ornament, ornamentDate, category, wallpaper)

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
        (clockStyle != ClockStyle.ORNAMENT || ornament == null || settings.ornament == ornament)
