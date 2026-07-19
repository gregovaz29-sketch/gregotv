package com.gregotv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.data.settings.AppSettings
import com.gregotv.data.settings.DefaultLists
import com.gregotv.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings(DefaultLists.IPTV, emptyList(), false)
    )

    fun addIptv(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch { repo.addIptvUrl(url) }
    }

    fun removeIptv(url: String) = viewModelScope.launch { repo.removeIptvUrl(url) }

    fun addSmb(path: String) {
        if (path.isBlank()) return
        viewModelScope.launch { repo.addSmbPath(path) }
    }

    fun removeSmb(path: String) = viewModelScope.launch { repo.removeSmbPath(path) }

    fun setAdult(enabled: Boolean) = viewModelScope.launch { repo.setAdultEnabled(enabled) }
}
