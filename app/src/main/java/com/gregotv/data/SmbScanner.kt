package com.gregotv.data

import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import jcifs.context.SingletonContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbScanner @Inject constructor() {

    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "m4v", "webm", "ts", "flv")

    /**
     * Recursively list video files under an smb:// path. Never throws; returns an
     * empty list on any failure (unreachable host, auth, permissions).
     * Path format: smb://user:pass@host/share/dir/  or  smb://host/share/
     */
    suspend fun scan(smbPath: String, maxDepth: Int = 3): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<MediaItem>()
            try {
                val ctx = SingletonContext.getInstance()
                val root = SmbFile(if (smbPath.endsWith("/")) smbPath else "$smbPath/", ctx)
                walk(root, results, maxDepth)
            } catch (e: Exception) {
                // swallow: SMB source is optional
            }
            results
        }

    private fun walk(dir: SmbFile, out: MutableList<MediaItem>, depth: Int) {
        if (depth < 0) return
        val children = try {
            dir.listFiles()
        } catch (e: Exception) {
            return
        } ?: return
        for (child in children) {
            try {
                if (child.isDirectory) {
                    walk(child, out, depth - 1)
                } else {
                    val name = child.name
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in videoExt) {
                        val url = child.url.toString()
                        out += MediaItem(
                            id = hash(url),
                            title = name.substringBeforeLast('.'),
                            url = url,
                            type = MediaType.SMB,
                            group = "Red / SMB"
                        )
                    }
                }
            } catch (e: Exception) {
                // skip unreadable entry
            }
        }
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
