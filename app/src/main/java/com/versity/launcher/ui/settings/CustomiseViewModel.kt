package com.versity.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.SettingsRepository
import com.versity.launcher.domain.ThemeBundle
import kotlinx.coroutines.launch

/** Write-side only: the current settings come from the home view model's state. */
class CustomiseViewModel(private val repo: SettingsRepository) : ViewModel() {
    fun setWallpaper(uri: String?) = viewModelScope.launch { repo.setWallpaperUri(uri) }
    fun setDim(percent: Int) = viewModelScope.launch { repo.setDimPercent(percent) }
    fun setClockStyle(style: ClockStyle) = viewModelScope.launch { repo.setClockStyle(style) }
    fun setFont(id: String) = viewModelScope.launch { repo.setFontId(id) }
    fun setPalette(id: String) = viewModelScope.launch { repo.setPaletteId(id) }
    fun setAccent(argb: Long?) = viewModelScope.launch { repo.setAccentArgb(argb) }
    fun setBackdrop(backdrop: com.versity.launcher.data.Backdrop) = viewModelScope.launch { repo.setBackdrop(backdrop) }
    fun setLayout(layout: HomeLayout) = viewModelScope.launch { repo.setLayout(layout) }
    fun setOrnament(o: com.versity.launcher.data.Ornament) = viewModelScope.launch { repo.setOrnament(o) }
    fun setOrnamentDate(show: Boolean) = viewModelScope.launch { repo.setOrnamentDate(show) }
    fun setLockStyle(enabled: Boolean) = viewModelScope.launch { repo.setLockStyleEnabled(enabled) }
    fun setFavourites(keys: List<String>) = viewModelScope.launch { repo.setFavourites(keys) }
    fun applyTheme(theme: ThemeBundle, clearWallpaper: Boolean = false) =
        viewModelScope.launch { repo.applyTheme(theme, clearWallpaper) }
    fun setLockTheme(id: String?) = viewModelScope.launch { repo.setLockThemeId(id) }
    fun setDrawerStyle(style: com.versity.launcher.data.DrawerStyle) = viewModelScope.launch { repo.setDrawerStyle(style) }
    fun setIconStyle(style: com.versity.launcher.data.IconStyle) = viewModelScope.launch { repo.setIconStyle(style) }
    fun reset() = viewModelScope.launch { repo.reset() }
}
