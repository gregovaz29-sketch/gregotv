package com.gregotv.data

import android.content.Context
import android.provider.MediaStore
import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val seriesPatternHints = listOf(
        Regex("""[sS]\d{1,2}[eE]\d{1,2}"""),
        Regex("""\b\d{1,2}x\d{1,2}\b"""),
        Regex("""(?i)temporada""")
    )

    /**
     * Query MediaStore for local videos. Classifies MOVIE vs SERIES by filename.
     * Requires READ_MEDIA_VIDEO (API 33+) / READ_EXTERNAL_STORAGE (<=32).
     * Returns an empty list if permission is missing.
     */
    suspend fun scan(): List<MediaItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val data = cursor.getString(dataCol) ?: continue
                    val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        .buildUpon().appendPath(id.toString()).build().toString()
                    val isSeries = seriesPatternHints.any { it.containsMatchIn(name) }
                    out += MediaItem(
                        id = hash(data),
                        title = name.substringBeforeLast('.'),
                        url = uri,
                        type = if (isSeries) MediaType.SERIES else MediaType.MOVIE,
                        group = if (isSeries) "Series (local)" else "Películas (local)"
                    )
                }
            }
        } catch (e: SecurityException) {
            // permission not granted yet
        } catch (e: Exception) {
            // ignore
        }
        out
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
