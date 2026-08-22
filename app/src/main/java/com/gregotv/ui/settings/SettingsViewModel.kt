package com.gregotv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregotv.data.LocalUploadServer
import com.gregotv.data.XtreamSource
import com.gregotv.data.settings.AppSettings
import com.gregotv.data.settings.DefaultLists
import com.gregotv.data.settings.SettingsRepository
import com.gregotv.ui.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.random.Random
import javax.inject.Inject

/** Displayed on the TV so the phone can reach the upload page. */
data class LocalServerInfo(val ip: String?, val port: Int, val pin: String)

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
            userIptvUrls = emptyList()
        )
    )

    /** Adds an M3U list. Returns false if it is not a usable URL. */
    fun addIptv(url: String): Boolean {
        val clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) return false
        safeLaunch("addIptv") { repo.addIptvUrl(clean) }
        return true
    }

    fun removeIptv(url: String) = safeLaunch("removeIptv") { repo.removeIptvUrl(url) }

    fun addSmb(path: String) {
        if (path.isBlank()) return
        safeLaunch("addSmb") { repo.addSmbPath(path) }
    }

    fun removeSmb(path: String) = safeLaunch("removeSmb") { repo.removeSmbPath(path) }

    fun setAdult(enabled: Boolean) = safeLaunch("setAdult") { repo.setAdultEnabled(enabled) }

    /** Adds an Xtream Codes account. Returns false if the input is incomplete. */
    fun addXtream(host: String, port: String, user: String, pass: String): Boolean {
        val cleanHost = host.trim()
            .removePrefix("https://").removePrefix("http://").trimEnd('/')
        val portNumber = port.trim().toIntOrNull() ?: 80
        if (cleanHost.isBlank() || user.isBlank() || pass.isBlank()) return false
        safeLaunch("addXtream") {
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
        safeLaunch("removeXtream") { repo.removeXtreamSource(source) }

    // ---- Local upload server --------------------------------------------
    // Started and stopped by the Settings screen's lifecycle, never in the
    // background. See LocalUploadServer.

    private val _server = MutableStateFlow<LocalServerInfo?>(null)
    val server: StateFlow<LocalServerInfo?> = _server.asStateFlow()

    private var running: LocalUploadServer? = null

    fun startServer() {
        if (running != null) return
        val pin = Random.nextInt(1000, 10000).toString()
        val started = LocalUploadServer.start(pin, repo) ?: return
        running = started
        _server.value = LocalServerInfo(
            ip = LocalUploadServer.localIpAddress(),
            port = started.listeningPort,
            pin = pin
        )
    }

    fun stopServer() {
        running?.let { server -> runCatching { server.stop() } }
        running = null
        _server.value = null
    }

    override fun onCleared() {
        stopServer()
        super.onCleared()
    }
}
