package com.gregotv.data.tmdb

import com.gregotv.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional TMDB metadata enrichment. Disabled when no API key is configured.
 * The app never blocks startup on this. Wire up Retrofit here if a key is set.
 */
@Singleton
class TmdbRepository @Inject constructor() {

    val enabled: Boolean get() = BuildConfig.TMDB_API_KEY.isNotBlank()

    /** Returns a poster URL for a title, or null when TMDB is disabled/unavailable. */
    suspend fun posterFor(title: String): String? {
        if (!enabled) return null
        // Intentionally left unimplemented: TMDB is off by default in this build.
        // With a key, add a Retrofit call to /search/movie here.
        return null
    }
}
