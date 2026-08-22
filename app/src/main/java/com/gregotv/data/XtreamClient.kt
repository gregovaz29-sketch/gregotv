package com.gregotv.data

import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credentials for one Xtream Codes account. Supplied entirely by the user in
 * Settings; nothing is bundled with the app.
 */
data class XtreamSource(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val https: Boolean = false
) {
    val scheme: String get() = if (https) "https" else "http"
    val baseUrl: String get() = "$scheme://$host:$port"

    /** Stable id used to key the source in storage. */
    val id: String get() = "$scheme://$username@$host:$port"

    fun serialize(): String = listOf(
        if (https) "https" else "http", host, port.toString(), username, password
    ).joinToString("|")

    companion object {
        fun parse(raw: String): XtreamSource? {
            val p = raw.split("|")
            if (p.size != 5) return null
            val port = p[2].toIntOrNull() ?: return null
            return XtreamSource(
                https = p[0] == "https",
                host = p[1],
                port = port,
                username = p[3],
                password = p[4]
            )
        }
    }
}

/**
 * Minimal client for the Xtream Codes `player_api.php` protocol, which most
 * IPTV panels expose. Generic and empty by default: the app ships no host,
 * no account and no defaults — the user enters their own provider details.
 *
 * Only live streams are mapped. VOD and series would need a real catalogue
 * UI, which this app does not have.
 */
@Singleton
class XtreamClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Fetch the account's live channels. Returns an empty list on any failure
     * so one bad source never aborts the whole load.
     */
    suspend fun liveChannels(source: XtreamSource): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val categories = runCatching { fetchCategories(source) }.getOrDefault(emptyMap())
            val raw = get(source, "get_live_streams") ?: return@withContext emptyList()

            runCatching {
                json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
                    val o = element.jsonObject
                    val streamId = o["stream_id"]?.jsonPrimitive?.intOrNull
                        ?: return@mapNotNull null
                    val name = o["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val categoryId = o["category_id"]?.jsonPrimitive?.contentOrNull
                    val streamUrl = streamUrl(source, streamId)

                    MediaItem(
                        id = hash(streamUrl),
                        title = name,
                        url = streamUrl,
                        type = MediaType.LIVE_CHANNEL,
                        posterUrl = o["stream_icon"]?.jsonPrimitive?.contentOrNull
                            ?.takeIf { it.isNotBlank() },
                        group = categories[categoryId],
                        origin = Genres.USER_SOURCES
                    )
                }
            }.getOrDefault(emptyList())
        }

    private fun fetchCategories(source: XtreamSource): Map<String, String> {
        val raw = get(source, "get_live_categories") ?: return emptyMap()
        return json.parseToJsonElement(raw).jsonArray.mapNotNull { element ->
            val o = element.jsonObject
            val id = o["category_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = o["category_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            id to name
        }.toMap()
    }

    /** Live stream URL in the shape every Xtream panel serves. */
    private fun streamUrl(source: XtreamSource, streamId: Int): String =
        "${source.baseUrl}/live/${enc(source.username)}/${enc(source.password)}/$streamId.m3u8"

    private fun get(source: XtreamSource, action: String): String? {
        val url = "${source.baseUrl}/player_api.php" +
            "?username=${enc(source.username)}&password=${enc(source.password)}" +
            "&action=$action"
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 15_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "GregoTV/1.0 (Android TV; Media3)")
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            text.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun hash(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
