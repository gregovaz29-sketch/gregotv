package com.gregotv.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.MediaRepository
import com.gregotv.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: MediaRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    fun observe(item: MediaItem) {
        viewModelScope.launch { _isFavorite.value = repo.isFavorite(item.id) }
    }

    fun toggleFavorite(item: MediaItem) {
        _isFavorite.value = !_isFavorite.value
        viewModelScope.launch { repo.toggleFavorite(item) }
    }
}
