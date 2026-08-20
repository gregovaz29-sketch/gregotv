package com.gregotv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.data.XtreamSource
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
        AppSettings(
            iptvUrls = DefaultLists.IPTV,
            smbPaths = emptyList(),
            adultEnabled = false,
            spanishOnly = true,
            userIptvUrls = emptyList()
        )
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

    fun setSpanishOnly(enabled: Boolean) =
        viewModelScope.launch { repo.setSpanishOnly(enabled) }

    /** Adds an Xtream Codes account. Returns false if the input is incomplete. */
    fun addXtream(host: String, port: String, user: String, pass: String): Boolean {
        val cleanHost = host.trim()
            .removePrefix("https://").removePrefix("http://").trimEnd('/')
        val portNumber = port.trim().toIntOrNull() ?: 80
        if (cleanHost.isBlank() || user.isBlank() || pass.isBlank()) return false
        viewModelScope.launch {
            repo.addXtreamSource(
                XtreamSource(
                    host = cleanHost,
                    port = portNumber,
                    username = user.trim(),
                    password = pass.trim(),
                    https = host.trim().startsWith("https://") || portNumber == 443
                )
            )
        }
        return true
    }

    fun removeXtream(source: XtreamSource) =
        viewModelScope.launch { repo.removeXtreamSource(source) }
}
