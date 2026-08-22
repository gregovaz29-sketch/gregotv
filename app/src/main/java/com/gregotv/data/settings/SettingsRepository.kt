package com.gregotv.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gregotv.data.XtreamSource
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
 * `index.m3u` (mostly English) and its `categories` lists (also mostly
 * English) and rely on the country/language lists plus tdtchannels for
 * Spanish-produced channels.
 *
 * The three `i.mjh.nz` FAST feeds (Pluto, Samsung TV+, Plex) used to live here
 * and were removed: that host retired its M3U playlists and now serves only
 * XMLTV EPG. All three URLs return 404 and the directory listing contains no
 * `.m3u8` at all.
 *
 * Note for future edits: Kotlin block comments nest, so a literal slash-star
 * inside this KDoc opens a second comment and swallows the rest of the file.
 * Write path globs without it.
 *
 * The CI diagnostics step parses this list directly, so keep the entries as
 * plain `https://...` string literals.
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
        "https://iptv-org.github.io/iptv/countries/pr.m3u"
    )
}

data class AppSettings(
    val iptvUrls: List<String>,
    val smbPaths: List<String>,
    val adultEnabled: Boolean,
    /** URLs the user added on top of DefaultLists. */
    val userIptvUrls: List<String> = emptyList(),
    /** Xtream Codes accounts the user configured. */
    val xtreamSources: List<XtreamSource> = emptyList()
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val iptvKey = stringSetPreferencesKey("iptv_urls")
    private val userIptvKey = stringSetPreferencesKey("user_iptv_urls")
    private val smbKey = stringSetPreferencesKey("smb_paths")
    private val xtreamKey = stringSetPreferencesKey("xtream_sources")
    private val adultKey = booleanPreferencesKey("adult_enabled")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val defaults = prefs[iptvKey]?.toList() ?: DefaultLists.IPTV
        val user = prefs[userIptvKey]?.toList().orEmpty()
        AppSettings(
            // Combined list drives the loader; the split is kept so the loader
            // can tell the user's own sources apart from the bundled ones.
            iptvUrls = (defaults + user).distinct(),
            smbPaths = prefs[smbKey]?.toList() ?: emptyList(),
            adultEnabled = prefs[adultKey] ?: false,
            userIptvUrls = user,
            xtreamSources = prefs[xtreamKey].orEmpty()
                .mapNotNull { XtreamSource.parse(it) }
        )
    }

    suspend fun addXtreamSource(source: XtreamSource) = context.dataStore.edit { prefs ->
        val current = prefs[xtreamKey] ?: emptySet()
        // Replace any existing entry for the same account rather than duplicating.
        val kept = current.filterNot { XtreamSource.parse(it)?.id == source.id }
        prefs[xtreamKey] = (kept + source.serialize()).toSet()
    }

    suspend fun removeXtreamSource(source: XtreamSource) = context.dataStore.edit { prefs ->
        val current = prefs[xtreamKey] ?: emptySet()
        prefs[xtreamKey] = current.filterNot { XtreamSource.parse(it)?.id == source.id }.toSet()
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
}
