package com.gregotv.data

import com.gregotv.data.settings.SettingsRepository
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * A tiny web server that runs only while the Settings screen is on screen, so a
 * phone on the same network can paste M3U list URLs instead of the user typing
 * them with a D-pad. Not a background service: [Lifecycle] starts and stops it.
 *
 * Access is gated by a 4-digit PIN shown on the TV. It is not security — it
 * only stops someone else on the same Wi-Fi from adding junk by guessing the
 * IP. All it can do is add or remove the user's own list URLs; there is nothing
 * sensitive to reach.
 *
 * Lists go only to DataStore, as the user's own sources. Never to DefaultLists.
 */
class LocalUploadServer(
    port: Int,
    private val pin: String,
    private val repo: SettingsRepository
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return when (session.method) {
            Method.POST -> handlePost(session)
            else -> page(message = null)
        }
    }

    private fun handlePost(session: IHTTPSession): Response {
        val body = HashMap<String, String>()
        runCatching { session.parseBody(body) }
        val params = session.parameters

        val givenPin = params["pin"]?.firstOrNull().orEmpty()
        if (givenPin != pin) {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "text/html; charset=utf-8",
                buildHtml("PIN incorrecto.")
            )
        }

        // "remove" wins if present: a delete button posts the URL to drop.
        val toRemove = params["remove"]?.firstOrNull()
        if (!toRemove.isNullOrBlank()) {
            runBlocking { repo.removeIptvUrl(toRemove) }
            return page(message = "Lista eliminada.")
        }

        val raw = params["urls"]?.firstOrNull().orEmpty()
        var added = 0
        val rejected = mutableListOf<String>()
        raw.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            if (line.startsWith("http://") || line.startsWith("https://")) {
                runBlocking { repo.addIptvUrl(line) }
                added++
            } else {
                rejected += line
            }
        }
        val msg = buildString {
            append("Añadidas: $added.")
            if (rejected.isNotEmpty()) {
                append(" Rechazadas ${rejected.size} (no empiezan por http:// o https://): ")
                append(rejected.take(5).joinToString(", "))
            }
        }
        return page(message = msg)
    }

    /** The upload page, including the user's current lists with delete buttons. */
    private fun page(message: String?): Response =
        newFixedLengthResponse(
            Response.Status.OK, "text/html; charset=utf-8", buildHtml(message)
        )

    private fun buildHtml(message: String?): String {
        val current = runCatching {
            runBlocking { repo.settings.first() }.userIptvUrls
        }.getOrDefault(emptyList())

        val listRows = if (current.isEmpty()) {
            "<p class=muted>Todavía no has añadido ninguna lista.</p>"
        } else current.joinToString("\n") { url ->
            """
            <div class="row">
              <span>${escape(url)}</span>
              <form method="post" class="inline">
                <input type="hidden" name="pin" value="__PIN__">
                <input type="hidden" name="remove" value="${escape(url)}">
                <button class="del">Borrar</button>
              </form>
            </div>
            """.trimIndent()
        }

        val notice = message?.let { "<div class=notice>${escape(it)}</div>" } ?: ""

        val html = """
            <!doctype html><html lang="es"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>GregoTV — añadir listas</title>
            <style>
              body{font-family:system-ui,sans-serif;max-width:640px;margin:24px auto;padding:0 16px;background:#141414;color:#eee}
              h1{color:#e50914}
              textarea{width:100%;height:140px;font-size:16px;background:#222;color:#eee;border:1px solid #444;border-radius:8px;padding:10px}
              input[type=text],input[type=number]{font-size:16px;padding:8px;background:#222;color:#eee;border:1px solid #444;border-radius:8px}
              button{font-size:16px;padding:10px 16px;background:#e50914;color:#fff;border:0;border-radius:8px;margin-top:10px}
              .del{background:#333;padding:6px 12px;margin:0}
              .row{display:flex;justify-content:space-between;align-items:center;gap:12px;border-bottom:1px solid #333;padding:8px 0;word-break:break-all}
              .inline{margin:0}
              .muted{color:#999}
              .notice{background:#223;border:1px solid #445;border-radius:8px;padding:10px;margin:12px 0}
            </style></head><body>
            <h1>GregoTV</h1>
            <p>Pega una o varias URLs de listas M3U, <b>una por línea</b>.</p>
            $notice
            <form method="post">
              <textarea name="urls" placeholder="https://...&#10;https://..."></textarea>
              <p>PIN (mostrado en la tele): <input type="text" name="pin" inputmode="numeric" maxlength="4" required></p>
              <button type="submit">Añadir</button>
            </form>
            <h3>Tus listas</h3>
            $listRows
            </body></html>
        """.trimIndent().replace("__PIN__", pin)

        return html
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    companion object {
        private const val FIRST_PORT = 8080
        private const val LAST_PORT = 8090

        /** Starts on the first free port in 8080..8090, or null if none is free. */
        fun start(pin: String, repo: SettingsRepository): LocalUploadServer? {
            for (port in FIRST_PORT..LAST_PORT) {
                val server = LocalUploadServer(port, pin, repo)
                try {
                    server.start(SOCKET_READ_TIMEOUT, false)
                    return server
                } catch (e: Exception) {
                    runCatching { server.stop() }
                }
            }
            return null
        }

        /**
         * The device's own LAN address. Read from NetworkInterface rather than
         * WifiManager, because many TV boxes are on Ethernet and WifiManager
         * reports nothing there.
         */
        fun localIpAddress(): String? {
            return runCatching {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                    .flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { it.isSiteLocalAddress }
                    ?.hostAddress
            }.getOrNull()
        }
    }
}
