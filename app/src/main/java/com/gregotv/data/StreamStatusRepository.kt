package com.gregotv.data

import android.content.Context
import com.gregotv.model.StreamVerification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the compact result produced by the scheduled GitHub verifier.
 *
 * This repository is deliberately best-effort: loading the catalogue never
 * waits for the network, and a failed download falls back to the last local
 * copy. The result only ranks duplicate stream variants; it never removes a
 * channel because a foreign CI runner can be geo-blocked.
 */
@Singleton
class StreamStatusRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .callTimeout(8, TimeUnit.SECONDS)
        .build()
    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE)

    @Volatile private var memory: Map<String, StreamVerification>? = null

    suspend fun verifiedUrls(): Map<String, StreamVerification> = withContext(Dispatchers.IO) {
        memory ?: load().also { memory = it }
    }

    fun statusOf(url: String, statuses: Map<String, StreamVerification>): StreamVerification =
        statuses[url] ?: StreamVerification.UNKNOWN

    private fun load(): Map<String, StreamVerification> {
        val downloaded = runCatching {
            val request = Request.Builder().url(STATUS_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.string()
            }
        }.getOrNull()

        val json = downloaded ?: runCatching { cacheFile.readText() }.getOrNull() ?: return emptyMap()
        if (downloaded != null) runCatching { cacheFile.writeText(downloaded) }

        return runCatching {
            val streams = JSONObject(json).optJSONObject("streams") ?: return@runCatching emptyMap()
            buildMap {
                val keys = streams.keys()
                while (keys.hasNext()) {
                    val url = keys.next()
                    val status = when (streams.optString(url).lowercase()) {
                        "ok" -> StreamVerification.OK
                        "dead" -> StreamVerification.DEAD
                        else -> StreamVerification.UNKNOWN
                    }
                    put(url, status)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private companion object {
        const val CACHE_FILE = "verified-streams.json"
        const val STATUS_URL =
            "https://raw.githubusercontent.com/gregovaz29-sketch/gregotv/data/verified.json"
    }
}
