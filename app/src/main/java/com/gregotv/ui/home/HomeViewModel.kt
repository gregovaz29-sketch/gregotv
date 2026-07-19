package com.gregotv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.MediaRepository
import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val hero: MediaItem? = null,
    val rows: List<ContentRowData> = emptyList()
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
                val hero = repo.heroItem(rows)
                _state.value = HomeState(
                    loading = false,
                    hero = hero,
                    rows = rows,
                    error = if (rows.isEmpty()) "No hay contenido disponible" else null
                )
            } catch (e: Exception) {
                _state.value = HomeState(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch { repo.toggleFavorite(item) }
    }
}
