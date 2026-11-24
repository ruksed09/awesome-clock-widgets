package com.versity.launcher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.AppsRepository
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.SettingsRepository
import com.versity.launcher.data.filterApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DrawerUiState(
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val loading: Boolean = true,
    val favouriteKeys: Set<String> = emptySet(),
) {
    val homeFull: Boolean get() = favouriteKeys.size >= LauncherSettings.MAX_FAVOURITES
}

class DrawerViewModel(
    private val repository: AppsRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val all = MutableStateFlow<List<AppEntry>?>(null)

    val state: StateFlow<DrawerUiState> = combine(query, all, settings.settings) { q, apps, s ->
        DrawerUiState(
            query = q,
            apps = filterApps(apps.orEmpty(), q),
            loading = apps == null,
            favouriteKeys = s.favourites.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    init { refresh() }

    /** Re-reads installed apps; call when the drawer opens so new installs appear. */
    fun refresh() {
        viewModelScope.launch { all.value = repository.loadApps() }
    }

    fun onQueryChange(value: String) { query.value = value }

    /** Adds the app to the home screen, or removes it if already there. Adding is ignored when home is full. */
    fun toggleFavourite(app: AppEntry) {
        viewModelScope.launch {
            val current = settings.settings.first().favourites
            val next = if (app.key in current) current - app.key else current + app.key
            settings.setFavourites(next)
        }
    }
}
