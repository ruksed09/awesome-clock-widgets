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
) {
    val uri: String get() = BundledWallpaper.uri(id)
}

/** Bundled wallpapers, rendered by tools/wallpapers (Leviathan is the user's artwork with its clock removed). */
object Wallpapers {
    val all: List<WallpaperOption> = listOf(
        WallpaperOption("leviathan", "Leviathan", "Celestial", R.drawable.wp_leviathan, R.drawable.wp_leviathan_thumb),
        WallpaperOption("crystal_sea", "Crystal sea", "Celestial", R.drawable.wp_crystal_sea, R.drawable.wp_crystal_sea_thumb),
        WallpaperOption("aurora_veil", "Aurora veil", "Celestial", R.drawable.wp_aurora_veil, R.drawable.wp_aurora_veil_thumb),
        WallpaperOption("moon_tide", "Moon tide", "Celestial", R.drawable.wp_moon_tide, R.drawable.wp_moon_tide_thumb),
        WallpaperOption("glass_bloom", "Glass bloom", "Glass", R.drawable.wp_glass_bloom, R.drawable.wp_glass_bloom_thumb, light = true),
        WallpaperOption("prism", "Prism", "Glass", R.drawable.wp_prism, R.drawable.wp_prism_thumb),
        WallpaperOption("silk", "Silk", "Glass", R.drawable.wp_silk, R.drawable.wp_silk_thumb),
        WallpaperOption("pebbles", "Pebbles", "Clay and earth", R.drawable.wp_pebbles, R.drawable.wp_pebbles_thumb, light = true),
        WallpaperOption("dusk_clay", "Dusk clay", "Clay and earth", R.drawable.wp_dusk_clay, R.drawable.wp_dusk_clay_thumb),
        WallpaperOption("dunes", "Dunes", "Clay and earth", R.drawable.wp_dunes, R.drawable.wp_dunes_thumb, light = true),
        WallpaperOption("twin_worlds", "Twin worlds", "Cosmos", R.drawable.wp_twin_worlds, R.drawable.wp_twin_worlds_thumb),
        WallpaperOption("nebula_veins", "Nebula veins", "Cosmos", R.drawable.wp_nebula_veins, R.drawable.wp_nebula_veins_thumb),
        WallpaperOption("ringed_giant", "Ringed giant", "Cosmos", R.drawable.wp_ringed_giant, R.drawable.wp_ringed_giant_thumb),
        WallpaperOption("supernova", "Supernova", "Cosmos", R.drawable.wp_supernova, R.drawable.wp_supernova_thumb),
    )

    val collections: List<String> = all.map { it.collection }.distinct()

    fun byId(id: String?): WallpaperOption? = all.firstOrNull { it.id == id }

    fun byUri(uri: String?): WallpaperOption? = byId(BundledWallpaper.idOf(uri))
}
