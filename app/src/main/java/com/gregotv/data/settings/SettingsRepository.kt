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

/** Legal, public IPTV lists shipped by default. Editable in Settings. */
object DefaultLists {
    val IPTV = listOf(
        "https://iptv-org.github.io/iptv/countries/es.m3u",
        "https://iptv-org.github.io/iptv/languages/spa.m3u",
        "https://iptv-org.github.io/iptv/index.m3u",
        "https://www.tdtchannels.com/lists/tv.m3u8",
        "https://iptv-org.github.io/iptv/categories/sports.m3u",
        "https://iptv-org.github.io/iptv/categories/news.m3u",
        "https://iptv-org.github.io/iptv/categories/movies.m3u",
        "https://iptv-org.github.io/iptv/categories/music.m3u",
        "https://i.mjh.nz/PlutoTV/es.m3u8",
        "https://i.mjh.nz/SamsungTVPlus/es.m3u8",
        "https://i.mjh.nz/Plex/es.m3u8",
        "https://iptv-org.github.io/iptv/countries/mx.m3u",
        "https://iptv-org.github.io/iptv/countries/ar.m3u",
        "https://iptv-org.github.io/iptv/countries/co.m3u",
        "https://iptv-org.github.io/iptv/countries/cl.m3u",
        "https://iptv-org.github.io/iptv/countries/pe.m3u",
        "https://iptv-org.github.io/iptv/countries/ve.m3u",
        "https://iptv-org.github.io/iptv/countries/ec.m3u",
        "https://iptv-org.github.io/iptv/countries/uy.m3u",
        "https://iptv-org.github.io/iptv/countries/pr.m3u",
        "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
        "https://iptv-org.github.io/iptv/categories/kids.m3u",
        "https://iptv-org.github.io/iptv/categories/comedy.m3u",
        "https://iptv-org.github.io/iptv/categories/documentary.m3u"
    )
}

data class AppSettings(
    val iptvUrls: List<String>,
    val smbPaths: List<String>,
    val adultEnabled: Boolean
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val iptvKey = stringSetPreferencesKey("iptv_urls")
    private val smbKey = stringSetPreferencesKey("smb_paths")
    private val adultKey = booleanPreferencesKey("adult_enabled")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            iptvUrls = prefs[iptvKey]?.toList() ?: DefaultLists.IPTV,
            smbPaths = prefs[smbKey]?.toList() ?: emptyList(),
            adultEnabled = prefs[adultKey] ?: false
        )
    }

    suspend fun addIptvUrl(url: String) = context.dataStore.edit { prefs ->
        val current = prefs[iptvKey] ?: DefaultLists.IPTV.toSet()
        prefs[iptvKey] = current + url.trim()
    }

    suspend fun removeIptvUrl(url: String) = context.dataStore.edit { prefs ->
        val current = prefs[iptvKey] ?: DefaultLists.IPTV.toSet()
        prefs[iptvKey] = current - url
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
}
