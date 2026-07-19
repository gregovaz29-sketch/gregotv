package com.gregotv.data

import com.gregotv.data.settings.DefaultLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ListDiagnostic(
    val url: String,
    val httpStatus: Int,
    val reachable: Boolean,
    val channels: Int
)

/**
 * Diagnostic pass over the default IPTV lists. For each list it records the HTTP
 * status and how many channels parsed (after NSFW filtering). Use to confirm the
 * lists are alive and produce playable channels. Runs each list concurrently.
 */
class M3uDiagnostics(private val parser: M3uParser) {

    suspend fun run(
        urls: List<String> = DefaultLists.IPTV,
        adultEnabled: Boolean = false
    ): List<ListDiagnostic> = coroutineScope {
        urls.map { url ->
            async(Dispatchers.IO) {
                val status = headStatus(url)
                val channels = parser.parse(url, adultEnabled).size
                ListDiagnostic(
                    url = url,
                    httpStatus = status,
                    reachable = status in 200..399 || channels > 0,
                    channels = channels
                )
            }
        }.map { it.await() }
    }

    fun report(results: List<ListDiagnostic>): String = buildString {
        val ok = results.count { it.reachable }
        val total = results.sumOf { it.channels }
        appendLine("GregoTV IPTV diagnostics")
        appendLine("Lists reachable: $ok/${results.size}")
        appendLine("Total channels parsed: $total")
        appendLine("-".repeat(60))
        results.forEach {
            appendLine("[HTTP ${it.httpStatus}] ${it.channels} canales  ${it.url}")
        }
    }

    private suspend fun headStatus(url: String): Int = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            conn.disconnect()
            code
        } catch (e: Exception) {
            -1
        }
    }
}
