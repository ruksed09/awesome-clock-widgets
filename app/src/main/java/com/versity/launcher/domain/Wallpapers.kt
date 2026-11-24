package com.versity.launcher.domain

import androidx.annotation.DrawableRes
import com.versity.launcher.R
import com.versity.launcher.data.BundledWallpaper

/** A wallpaper that ships with the app. [thumb] is a small copy for grids, so they never decode the full image. */
data class WallpaperOption(
    val id: String,
    val name: String,
    val collection: String,
    @DrawableRes val full: Int,
    @DrawableRes val thumb: Int,
    /** Pale image: pairs with a light palette (dark text). */
    val light: Boolean = false,
    /** Landscape rendering (2400x1500) for tablets and other wide screens; null crops the portrait image. */
    @DrawableRes val land: Int? = null,
) {
    val uri: String get() = BundledWallpaper.uri(id)
}

/** Bundled wallpapers, rendered by tools/wallpapers (Leviathan is the user's artwork with its clock removed). */
object Wallpapers {
    val all: List<WallpaperOption> = listOf(
        WallpaperOption("leviathan", "Leviathan", "Celestial", R.drawable.wp_leviathan, R.drawable.wp_leviathan_thumb),
        WallpaperOption("crystal_sea", "Crystal sea", "Celestial", R.drawable.wp_crystal_sea, R.drawable.wp_crystal_sea_thumb, land = R.drawable.wp_crystal_sea_land),
        WallpaperOption("aurora_veil", "Aurora veil", "Celestial", R.drawable.wp_aurora_veil, R.drawable.wp_aurora_veil_thumb, land = R.drawable.wp_aurora_veil_land),
        WallpaperOption("moon_tide", "Moon tide", "Celestial", R.drawable.wp_moon_tide, R.drawable.wp_moon_tide_thumb, land = R.drawable.wp_moon_tide_land),
        WallpaperOption("twin_worlds", "Twin worlds", "Cosmos", R.drawable.wp_twin_worlds, R.drawable.wp_twin_worlds_thumb, land = R.drawable.wp_twin_worlds_land),
        WallpaperOption("nebula_veins", "Nebula veins", "Cosmos", R.drawable.wp_nebula_veins, R.drawable.wp_nebula_veins_thumb, land = R.drawable.wp_nebula_veins_land),
        WallpaperOption("ringed_giant", "Ringed giant", "Cosmos", R.drawable.wp_ringed_giant, R.drawable.wp_ringed_giant_thumb, land = R.drawable.wp_ringed_giant_land),
        WallpaperOption("supernova", "Supernova", "Cosmos", R.drawable.wp_supernova, R.drawable.wp_supernova_thumb, land = R.drawable.wp_supernova_land),
        WallpaperOption("mesh_dawn", "Dawn mesh", "Minimal", R.drawable.wp_mesh_dawn, R.drawable.wp_mesh_dawn_thumb, light = true, land = R.drawable.wp_mesh_dawn_land),
        WallpaperOption("mesh_ink", "Ink mesh", "Minimal", R.drawable.wp_mesh_ink, R.drawable.wp_mesh_ink_thumb, land = R.drawable.wp_mesh_ink_land),
        WallpaperOption("calm_sea", "Calm sea", "Minimal", R.drawable.wp_calm_sea, R.drawable.wp_calm_sea_thumb, light = true, land = R.drawable.wp_calm_sea_land),
        WallpaperOption("oled_ring", "Halo", "Minimal", R.drawable.wp_oled_ring, R.drawable.wp_oled_ring_thumb, land = R.drawable.wp_oled_ring_land),
        WallpaperOption("contour", "Contour", "Line art", R.drawable.wp_contour, R.drawable.wp_contour_thumb, land = R.drawable.wp_contour_land),
        WallpaperOption("waveform", "Ridgeline", "Line art", R.drawable.wp_waveform, R.drawable.wp_waveform_thumb, land = R.drawable.wp_waveform_land),
        WallpaperOption("bauhaus", "Bauhaus", "Line art", R.drawable.wp_bauhaus, R.drawable.wp_bauhaus_thumb, light = true, land = R.drawable.wp_bauhaus_land),
        WallpaperOption("paper_layers", "Papercut", "Landscapes", R.drawable.wp_paper_layers, R.drawable.wp_paper_layers_thumb, light = true, land = R.drawable.wp_paper_layers_land),
        WallpaperOption("misty_peaks", "Misty peaks", "Landscapes", R.drawable.wp_misty_peaks, R.drawable.wp_misty_peaks_thumb, land = R.drawable.wp_misty_peaks_land),
        WallpaperOption("dunes", "Dunes", "Landscapes", R.drawable.wp_dunes, R.drawable.wp_dunes_thumb, light = true, land = R.drawable.wp_dunes_land),
        WallpaperOption("glass_bloom", "Glass bloom", "Glass and material", R.drawable.wp_glass_bloom, R.drawable.wp_glass_bloom_thumb, light = true, land = R.drawable.wp_glass_bloom_land),
        WallpaperOption("glass_orbs", "Glass orbs", "Glass and material", R.drawable.wp_glass_orbs, R.drawable.wp_glass_orbs_thumb, land = R.drawable.wp_glass_orbs_land),
        WallpaperOption("prism", "Prism", "Glass and material", R.drawable.wp_prism, R.drawable.wp_prism_thumb, land = R.drawable.wp_prism_land),
        WallpaperOption("liquid_chrome", "Liquid chrome", "Glass and material", R.drawable.wp_liquid_chrome, R.drawable.wp_liquid_chrome_thumb, land = R.drawable.wp_liquid_chrome_land),
        WallpaperOption("iridescent", "Iridescent", "Glass and material", R.drawable.wp_iridescent, R.drawable.wp_iridescent_thumb, light = true, land = R.drawable.wp_iridescent_land),
        WallpaperOption("silk", "Silk", "Glass and material", R.drawable.wp_silk, R.drawable.wp_silk_thumb, land = R.drawable.wp_silk_land),
        WallpaperOption("pebbles", "Pebbles", "Clay", R.drawable.wp_pebbles, R.drawable.wp_pebbles_thumb, light = true, land = R.drawable.wp_pebbles_land),
        WallpaperOption("dusk_clay", "Dusk clay", "Clay", R.drawable.wp_dusk_clay, R.drawable.wp_dusk_clay_thumb, land = R.drawable.wp_dusk_clay_land),
    )

    val collections: List<String> = all.map { it.collection }.distinct()

    fun byId(id: String?): WallpaperOption? = all.firstOrNull { it.id == id }

    fun byUri(uri: String?): WallpaperOption? = byId(BundledWallpaper.idOf(uri))

    /**
     * The best source for a bundled wallpaper drawn at [widthPx] x [heightPx]: the landscape rendering on
     * wide screens, the thumbnail for small previews, otherwise the full portrait image. Other URIs pass through.
     */
    fun sourceFor(uri: String?, widthPx: Int, heightPx: Int): String? {
        val wp = byUri(uri) ?: return uri
        return when {
            wp.land != null && widthPx > heightPx * 1.05f -> BundledWallpaper.uri("${wp.id}_land")
            widthPx in 1..400 -> BundledWallpaper.uri("${wp.id}_thumb")
            else -> wp.uri
        }
    }
}
