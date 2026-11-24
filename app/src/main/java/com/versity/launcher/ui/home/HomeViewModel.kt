package com.versity.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.AppsRepository
import com.versity.launcher.data.SettingsRepository
import com.versity.launcher.data.resolveFavourites
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    settings: SettingsRepository,
    private val apps: AppsRepository,
    clock: Flow<LocalDateTime>,
    isDefaultLauncher: Flow<Boolean>,
    is24Hour: () -> Boolean,
) : ViewModel() {
    /** Dismissal lasts until the process dies, so the banner never nags in a loop. */
    private val bannerDismissed = MutableStateFlow(false)
    private val installed = MutableStateFlow<List<AppEntry>>(emptyList())

    private val base: Flow<HomeUiState> = combine(
        settings.settings, clock, isDefaultLauncher, bannerDismissed,
    ) { s, now, isDefault, dismissed ->
        HomeUiState(
            settings = s, now = now, isDefaultLauncher = isDefault,
            is24Hour = is24Hour(), defaultBannerDismissed = dismissed, loaded = true,
        )
    }

    /** Starts not loaded, so the first frames draw plain black instead of the default palette. */
    val state: StateFlow<HomeUiState> = combine(base, installed) { b, list ->
        b.copy(favourites = resolveFavourites(b.settings.favourites, list))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(now = LocalDateTime.now(), loaded = false))

    init { refreshApps() }

    /** Re-reads installed apps (new installs, removed apps) so favourites stay accurate. */
    fun refreshApps() {
        viewModelScope.launch { installed.value = apps.loadApps() }
    }

    fun dismissBanner() { bannerDismissed.value = true }
}
