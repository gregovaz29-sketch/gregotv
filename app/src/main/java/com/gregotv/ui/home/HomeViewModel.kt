package com.gregotv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.MediaRepository
import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem
import com.gregotv.ui.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    /** Billboard rotation; first entry is what shows on arrival. */
    val heroes: List<MediaItem> = emptyList(),
    val rows: List<ContentRowData> = emptyList(),
    /** Ids currently in "Mi lista", so the billboard button reflects state. */
    val favoriteIds: Set<String> = emptySet()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = HomeState(loading = true)
        viewModelScope.launch {
            try {
                val rows = repo.loadRows()
                _state.value = HomeState(
                    loading = false,
                    heroes = repo.heroItems(rows),
                    rows = rows,
                    favoriteIds = rows
                        .firstOrNull { it.title == "Mis favoritos" }
                        ?.items?.map { it.id }?.toSet()
                        .orEmpty(),
                    error = if (rows.isEmpty()) "No hay contenido disponible" else null
                )
            } catch (e: Exception) {
                _state.value = HomeState(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        // Flip locally first so the button responds immediately; the row list
        // itself refreshes on the next load().
        _state.update { s ->
            s.copy(
                favoriteIds = if (item.id in s.favoriteIds) {
                    s.favoriteIds - item.id
                } else {
                    s.favoriteIds + item.id
                }
            )
        }
        safeLaunch("toggleFavorite") { repo.toggleFavorite(item) }
    }
}
