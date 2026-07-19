package com.gregotv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.MediaRepository
import com.gregotv.model.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repo: MediaRepository
) : ViewModel() {

    suspend fun startPosition(id: String): Long = repo.savedPosition(id)

    fun save(item: MediaItem, positionMs: Long, durationMs: Long) {
        viewModelScope.launch { repo.saveProgress(item, positionMs, durationMs) }
    }
}
