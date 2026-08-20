package com.gregotv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "gregotv_settings")

/**
 * Legal, public IPTV lists shipped by default. Editable in Settings.
 *
 * Curated for Spanish content only. We deliberately drop iptv-org's global
 * `index.m3u` (mostly English) and the `categories/*` lists (also mostly
 * English) and rely on the country/language lists plus tdtchannels for
 * Spanish-produced channels; the three `i.mjh.nz/*/es.m3u8` FAST feeds bring
 * Pluto/Samsung/Plex movie and series channels in Spanish.
 */
object DefaultLists {
    val IPTV = listOf(
        // Spain
        "https://iptv-org.github.io/iptv/countries/es.m3u",
        "https://iptv-org.github.io/iptv/languages/spa.m3u",
        "https://www.tdtchannels.com/lists/tv.m3u8",
        // Latin America
        "https://iptv-org.github.io/iptv/countries/mx.m3u",
        "https://iptv-org.github.io/iptv/countries/ar.m3u",
        "https://iptv-org.github.io/iptv/countries/co.m3u",
        "https://iptv-org.github.io/iptv/countries/cl.m3u",
        "https://iptv-org.github.io/iptv/countries/pe.m3u",
        "https://iptv-org.github.io/iptv/countries/ve.m3u",
        "https://iptv-org.github.io/iptv/countries/ec.m3u",
        "https://iptv-org.github.io/iptv/countries/uy.m3u",
        "https://iptv-org.github.io/iptv/countries/pr.m3u",
        // FAST (movies/series in Spanish)
        "https://i.mjh.nz/PlutoTV/es.m3u8",
        "https://i.mjh.nz/SamsungTVPlus/es.m3u8",
        "https://i.mjh.nz/Plex/es.m3u8"
    )
}

data class AppSettings(
    val iptvUrls: List<String>,
    val smbPaths: List<String>,
    val adultEnabled: Boolean,
    /**
     * When true, non-Spanish channels from the default lists are hidden.
     * User-added M3U/Xtream sources are always shown regardless of this flag —
     * if the user added them, they want them.
     */
    val spanishOnly: Boolean,
    /** URLs the user added on top of DefaultLists, exempt from spanishOnly. */
    val userIptvUrls: List<String> = emptyList()
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val iptvKey = stringSetPreferencesKey("iptv_urls")
    private val userIptvKey = stringSetPreferencesKey("user_iptv_urls")
    private val smbKey = stringSetPreferencesKey("smb_paths")
    private val adultKey = booleanPreferencesKey("adult_enabled")
    private val spanishOnlyKey = booleanPreferencesKey("spanish_only")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = prefs[iptvKey]?.toList() ?: DefaultLists.IPTV
        val user = prefs[userIptvKey]?.toList().orEmpty()
        AppSettings(
            // Combined list drives the loader; the user split is kept so the
            // spanishOnly filter can exempt user URLs.
            iptvUrls = (defaults + user).distinct(),
            smbPaths = prefs[smbKey]?.toList() ?: emptyList(),
            adultEnabled = prefs[adultKey] ?: false,
            spanishOnly = prefs[spanishOnlyKey] ?: true,
            userIptvUrls = user
        )
    }

    /** Adds a URL as a user source (exempt from the Spanish filter). */
    suspend fun addIptvUrl(url: String) = context.dataStore.edit { prefs ->
        val current = prefs[userIptvKey] ?: emptySet()
        prefs[userIptvKey] = current + url.trim()
    }

    /**
     * Removes a URL. If the URL is a default it is removed from the default
     * set (persisted so the removal survives); if it is a user URL it is
     * removed from the user set.
     */
    suspend fun removeIptvUrl(url: String) = context.dataStore.edit { prefs ->
        val user = prefs[userIptvKey] ?: emptySet()
        if (url in user) {
            prefs[userIptvKey] = user - url
        } else {
            val defaults = prefs[iptvKey] ?: DefaultLists.IPTV.toSet()
            prefs[iptvKey] = defaults - url
        }
    }

    suspend fun addSmbPath(path: String) = context.dataStore.edit { prefs ->
        val current = prefs[smbKey] ?: emptySet()
        prefs[smbKey] = current + path.trim()
    }

    suspend fun removeSmbPath(path: String) = context.dataStore.edit { prefs ->
        val current = prefs[smbKey] ?: emptySet()
        prefs[smbKey] = current - path
    }

    suspend fun setAdultEnabled(enabled: Boolean) = context.dataStore.edit { prefs ->
        prefs[adultKey] = enabled
    }

    suspend fun setSpanishOnly(enabled: Boolean) = context.dataStore.edit { prefs ->
        prefs[spanishOnlyKey] = enabled
    }
}
