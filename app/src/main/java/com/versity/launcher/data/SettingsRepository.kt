package com.versity.launcher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.versity.launcher.domain.ThemeBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore wrapper. Each setting is its own key so one field can change without
 * rewriting the others. Absent keys fall back to [LauncherSettings.DEFAULT].
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<LauncherSettings> = dataStore.data.map { prefs ->
        val d = LauncherSettings.DEFAULT
        LauncherSettings(
            // Absent means never chosen (the default wallpaper); an empty string means "no wallpaper".
            wallpaperUri = prefs[WALLPAPER_URI].let { if (it == null) d.wallpaperUri else it.ifEmpty { null } },
            dimPercent = (prefs[DIM_PERCENT] ?: d.dimPercent)
                .coerceIn(LauncherSettings.MIN_DIM, LauncherSettings.MAX_DIM),
            clockStyle = prefs[CLOCK_STYLE]
                ?.let { name -> ClockStyle.entries.firstOrNull { it.name == name } }
                ?: d.clockStyle,
            fontId = prefs[FONT_ID] ?: d.fontId,
            paletteId = prefs[PALETTE_ID] ?: d.paletteId,
            accentArgb = prefs[ACCENT_ARGB],
            layout = prefs[LAYOUT]
                ?.let { name -> HomeLayout.entries.firstOrNull { it.name == name } }
                ?: d.layout,
            backdrop = prefs[BACKDROP]
                ?.let { name -> Backdrop.entries.firstOrNull { it.name == name } }
                ?: d.backdrop,
            favourites = prefs[FAVOURITES]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: d.favourites,
            lockStyleEnabled = prefs[LOCK_STYLE] ?: d.lockStyleEnabled,
            ornament = prefs[ORNAMENT]
                ?.let { name -> Ornament.entries.firstOrNull { it.name == name } }
                ?: d.ornament,
            ornamentDate = prefs[ORNAMENT_DATE] ?: d.ornamentDate,
            lockThemeId = prefs[LOCK_THEME],
            lockWallpaperUri = prefs[LOCK_WALLPAPER],
            drawerStyle = prefs[DRAWER_STYLE]
                ?.let { name -> DrawerStyle.entries.firstOrNull { it.name == name } }
                ?: d.drawerStyle,
            iconStyle = prefs[ICON_STYLE]
                ?.let { name -> IconStyle.entries.firstOrNull { it.name == name } }
                ?: d.iconStyle,
            clockPlacement = ClockPlacement.decode(prefs[CLOCK_PLACEMENT]),
            lockClockPlacement = ClockPlacement.decode(prefs[LOCK_CLOCK_PLACEMENT]),
            uiMode = prefs[UI_MODE]
                ?.let { name -> UiMode.entries.firstOrNull { it.name == name } }
                ?: d.uiMode,
        )
    }

    suspend fun setWallpaperUri(uri: String?) = set(WALLPAPER_URI, uri ?: "")

    suspend fun setLockWallpaperUri(uri: String?) = set(LOCK_WALLPAPER, uri)

    suspend fun setDimPercent(percent: Int) =
        set(DIM_PERCENT, percent.coerceIn(LauncherSettings.MIN_DIM, LauncherSettings.MAX_DIM))

    suspend fun setClockStyle(style: ClockStyle) = set(CLOCK_STYLE, style.name)

    suspend fun setFontId(id: String) = set(FONT_ID, id)

    suspend fun setPaletteId(id: String) = set(PALETTE_ID, id)

    suspend fun setAccentArgb(argb: Long?) = set(ACCENT_ARGB, argb)

    suspend fun setLayout(layout: HomeLayout) = set(LAYOUT, layout.name)

    /** Keeps order, drops duplicates, and stops at [LauncherSettings.MAX_FAVOURITES]. */
    suspend fun setFavourites(keys: List<String>) {
        val clean = keys.distinct().take(LauncherSettings.MAX_FAVOURITES)
        set(FAVOURITES, clean.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR))
    }

    suspend fun setBackdrop(backdrop: Backdrop) = set(BACKDROP, backdrop.name)

    suspend fun setLockStyleEnabled(enabled: Boolean) = set(LOCK_STYLE, enabled)

    suspend fun setOrnament(ornament: Ornament) = set(ORNAMENT, ornament.name)

    suspend fun setOrnamentDate(show: Boolean) = set(ORNAMENT_DATE, show)

    /** null makes the lock screen follow the home screen again. */
    suspend fun setLockThemeId(id: String?) = set(LOCK_THEME, id)

    suspend fun setDrawerStyle(style: DrawerStyle) = set(DRAWER_STYLE, style.name)

    suspend fun setIconStyle(style: IconStyle) = set(ICON_STYLE, style.name)

    suspend fun setUiMode(mode: UiMode) = set(UI_MODE, mode.name)

    /** Both placements in one write, so the editor's Done is atomic. Null returns a clock to its layout. */
    suspend fun setClockPlacements(home: ClockPlacement?, lock: ClockPlacement?) {
        dataStore.edit { p ->
            if (home == null) p.remove(CLOCK_PLACEMENT) else p[CLOCK_PLACEMENT] = home.clamped().encode()
            if (lock == null) p.remove(LOCK_CLOCK_PLACEMENT) else p[LOCK_CLOCK_PLACEMENT] = lock.clamped().encode()
        }
    }

    /**
     * One transaction: look only. Favourites, lock-style, drawer and icon choices are left alone.
     * The current wallpaper is kept unless [replaceWallpaper] asks for the theme's own (its bundled
     * wallpaper, or none so its gradient and backdrop show).
     */
    suspend fun applyTheme(theme: ThemeBundle, replaceWallpaper: Boolean = false) {
        dataStore.edit { p ->
            if (replaceWallpaper) p[WALLPAPER_URI] = theme.wallpaperId?.let(BundledWallpaper::uri) ?: ""
            p[PALETTE_ID] = theme.paletteId
            p[FONT_ID] = theme.fontId
            p[CLOCK_STYLE] = theme.clockStyle.name
            p[DIM_PERCENT] = theme.dimPercent.coerceIn(LauncherSettings.MIN_DIM, LauncherSettings.MAX_DIM)
            p[LAYOUT] = theme.layout.name
            p[BACKDROP] = theme.backdrop.name
            if (theme.accentArgb == null) p.remove(ACCENT_ARGB) else p[ACCENT_ARGB] = theme.accentArgb
            theme.ornament?.let { p[ORNAMENT] = it.name }
            theme.ornamentDate?.let { p[ORNAMENT_DATE] = it }
        }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(key) else prefs[key] = value }
    }

    private companion object {
        const val SEPARATOR = "\n"
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val DIM_PERCENT = intPreferencesKey("dim_percent")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val FONT_ID = stringPreferencesKey("font_id")
        val PALETTE_ID = stringPreferencesKey("palette_id")
        val ACCENT_ARGB = longPreferencesKey("accent_argb")
        val LAYOUT = stringPreferencesKey("layout")
        val BACKDROP = stringPreferencesKey("backdrop")
        val FAVOURITES = stringPreferencesKey("favourites")
        val LOCK_STYLE = booleanPreferencesKey("lock_style_enabled")
        val ORNAMENT = stringPreferencesKey("ornament")
        val ORNAMENT_DATE = booleanPreferencesKey("ornament_date")
        val LOCK_THEME = stringPreferencesKey("lock_theme_id")
        val LOCK_WALLPAPER = stringPreferencesKey("lock_wallpaper_uri")
        val DRAWER_STYLE = stringPreferencesKey("drawer_style")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val UI_MODE = stringPreferencesKey("ui_mode")
        val CLOCK_PLACEMENT = stringPreferencesKey("clock_placement")
        val LOCK_CLOCK_PLACEMENT = stringPreferencesKey("lock_clock_placement")
    }
}
