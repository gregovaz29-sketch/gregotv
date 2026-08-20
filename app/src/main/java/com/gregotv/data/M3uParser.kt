package com.gregotv.data

import android.content.Context
import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3uParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val blockedGroups = setOf("xxx", "adult", "porn", "porno", "18+")
    private val cacheDir: File by lazy { File(context.cacheDir, "m3u").apply { mkdirs() } }

    /** ISO country codes of Spanish-speaking countries, matched inside tvg-id. */
    private val spanishCountries = setOf(
        "es", "mx", "ar", "co", "cl", "ve", "pe", "ec", "gt", "cu", "bo",
        "do", "hn", "py", "sv", "ni", "cr", "pa", "uy", "gq"
    )
    // tvg-id looks like "Name.cc@Quality" or "Name.cc"; grab the trailing code.
    private val idCountry = Regex("""\.([a-z]{2})(?:@|$)""", RegexOption.IGNORE_CASE)

    /**
     * A channel counts as Spanish when it came from a Spanish list, or its
     * tvg-language / tvg-id says so. index.m3u carries no language tag, so the
     * tvg-id country code is the main signal there.
     */
    private fun isSpanish(origin: String, language: String?, tvgId: String?): Boolean {
        if (origin == "España" || origin == "En español" || origin == "Latinoamérica") {
            return true
        }
        val lang = language?.lowercase().orEmpty()
        if ("spa" in lang || "spanish" in lang || "castellano" in lang || "espa" in lang) {
            return true
        }
        val code = tvgId?.let { idCountry.find(it)?.groupValues?.get(1)?.lowercase() }
        return code in spanishCountries
    }

    private fun isAllowed(groupTitle: String?, adultEnabled: Boolean): Boolean {
        if (adultEnabled) return true
        val g = groupTitle?.lowercase() ?: return true
        return blockedGroups.none { it in g }
    }

    /**
     * Fetch and parse one M3U list. On network failure, fall back to the last
     * cached copy so the app can start offline. Returns an empty list if nothing
     * is available. Never throws.
     */
    suspend fun parse(url: String, adultEnabled: Boolean): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val raw = fetch(url) ?: readCache(url) ?: return@withContext emptyList()
            parseText(raw, adultEnabled, url)
        }

    private fun fetch(url: String): String? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            if (text.isNotBlank()) writeCache(url, text)
            text
        } catch (e: Exception) {
            null
        }
    }

    private fun parseText(
        text: String,
        adultEnabled: Boolean,
        listUrl: String
    ): List<MediaItem> {
        val result = LinkedHashMap<String, MediaItem>()
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        var pendingTitle: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var pendingLang: String? = null
        var pendingId: String? = null

        for (line in lines) {
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingTitle = extractDisplayName(line)
                pendingLogo = extractAttr(line, "tvg-logo")
                pendingGroup = extractAttr(line, "group-title")
                pendingLang = extractAttr(line, "tvg-language")
                pendingId = extractAttr(line, "tvg-id")
            } else if (line.startsWith("#")) {
                // ignore other directives (#EXTM3U, #EXTVLCOPT, etc.)
            } else {
                val streamUrl = line
                if (streamUrl.isBlank()) continue
                if (!isAllowed(pendingGroup, adultEnabled)) {
                    pendingTitle = null; pendingLogo = null; pendingGroup = null
                    pendingLang = null; pendingId = null
                    continue
                }
                val title = pendingTitle ?: streamUrl.substringAfterLast('/')
                // The entry's own tvg-id country code beats the list URL here.
                val origin = Genres.originFor(listUrl, pendingId)
                // dedupe by url
                result[streamUrl] = MediaItem(
                    id = hash(streamUrl),
                    title = title,
                    url = streamUrl,
                    type = MediaType.LIVE_CHANNEL,
                    posterUrl = pendingLogo?.takeIf { it.isNotBlank() },
                    // Left null when absent: a filler group like "General" would
                    // otherwise be treated as a real genre for thousands of entries.
                    group = pendingGroup?.takeIf { it.isNotBlank() },
                    origin = origin,
                    spanish = isSpanish(origin, pendingLang, pendingId)
                )
                pendingTitle = null; pendingLogo = null; pendingGroup = null
                pendingLang = null; pendingId = null
            }
        }
        return result.values.toList()
    }

    private fun extractAttr(line: String, attr: String): String? {
        val regex = Regex("""$attr="([^"]*)"""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun extractDisplayName(line: String): String {
        val comma = line.lastIndexOf(',')
        return if (comma >= 0 && comma < line.length - 1) {
            line.substring(comma + 1).trim()
        } else "Canal"
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun cacheFile(url: String) = File(cacheDir, hash(url) + ".m3u")
    private fun writeCache(url: String, text: String) =
        runCatching { cacheFile(url).writeText(text) }
    private fun readCache(url: String): String? =
        cacheFile(url).takeIf { it.exists() }?.readText()
}
