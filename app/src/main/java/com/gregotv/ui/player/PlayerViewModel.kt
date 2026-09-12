package com.gregotv.ui.player

import androidx.lifecycle.ViewModel
import com.gregotv.MediaRepository
import com.gregotv.model.MediaItem
import com.gregotv.ui.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repo: MediaRepository
) : ViewModel() {

    suspend fun startPosition(id: String): Long = repo.savedPosition(id)

    fun save(item: MediaItem, positionMs: Long, durationMs: Long) {
        safeLaunch("saveProgress") { repo.saveProgress(item, positionMs, durationMs) }
    }

    /** Mark a live channel URL as dead so the loader hides it for a while. */
    fun reportFailure(url: String) {
        safeLaunch("reportFailure") { repo.reportChannelFailure(url) }
    }

    fun reportSuccess(url: String) {
        safeLaunch("reportSuccess") { repo.reportChannelSuccess(url) }
    }
}
