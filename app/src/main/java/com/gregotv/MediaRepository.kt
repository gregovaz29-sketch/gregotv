package com.gregotv

import com.gregotv.data.LocalScanner
import com.gregotv.data.M3uParser
import com.gregotv.data.SmbScanner
import com.gregotv.data.db.FavoriteDao
import com.gregotv.data.db.FavoriteEntity
import com.gregotv.data.db.ProgressDao
import com.gregotv.data.db.ProgressEntity
import com.gregotv.data.settings.SettingsRepository
import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val m3uParser: M3uParser,
    private val smbScanner: SmbScanner,
    private val localScanner: LocalScanner,
    private val settingsRepo: SettingsRepository,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao
) {
    /**
     * Load everything and group into rows for the home screen. Each source is
     * fetched concurrently; a failing source contributes nothing but never aborts
     * the whole load.
     */
    suspend fun loadRows(): List<ContentRowData> = coroutineScope {
        val settings = settingsRepo.settings.first()

        val iptvDeferred = settings.iptvUrls.map { url ->
            async { m3uParser.parse(url, settings.adultEnabled) }
        }
        val smbDeferred = settings.smbPaths.map { path ->
            async { smbScanner.scan(path) }
        }
        val localDeferred = async { localScanner.scan() }

        val iptv = iptvDeferred.flatMap { it.await() }
        val smb = smbDeferred.flatMap { it.await() }
        val local = localDeferred.await()

        val rows = mutableListOf<ContentRowData>()

        // Continue watching first
        val progress = progressDao.observeRecent().first()
        if (progress.isNotEmpty()) {
            rows += ContentRowData(
                "Continuar viendo",
                progress.map { it.toMediaItem() }
            )
        }

        // Favorites
        val favs = favoriteDao.observeAll().first()
        if (favs.isNotEmpty()) {
            rows += ContentRowData("Mis favoritos", favs.map { it.toMediaItem() })
        }

        // Local movies / series
        local.filter { it.type == MediaType.MOVIE }.takeIf { it.isNotEmpty() }?.let {
            rows += ContentRowData("Películas", it)
        }
        local.filter { it.type == MediaType.SERIES }.takeIf { it.isNotEmpty() }?.let {
            rows += ContentRowData("Series", it)
        }

        // SMB
        if (smb.isNotEmpty()) rows += ContentRowData("Red / SMB", smb)

        // Live channels grouped by group-title, biggest groups first
        iptv.groupBy { it.group ?: "General" }
            .entries
            .sortedByDescending { it.value.size }
            .forEach { (group, items) ->
                rows += ContentRowData(group, items.take(60))
            }

        rows
    }

    /** Pick a hero item: first favorite, else first live channel with a logo. */
    suspend fun heroItem(rows: List<ContentRowData>): MediaItem? {
        val all = rows.flatMap { it.items }
        return all.firstOrNull { it.posterUrl != null } ?: all.firstOrNull()
    }

    // Favorites
    fun favorites(): Flow<List<MediaItem>> =
        favoriteDao.observeAll().map { list -> list.map { it.toMediaItem() } }

    suspend fun isFavorite(id: String) = favoriteDao.isFavorite(id)

    suspend fun toggleFavorite(item: MediaItem) {
        if (favoriteDao.isFavorite(item.id)) {
            favoriteDao.remove(item.id)
        } else {
            favoriteDao.add(item.toFavoriteEntity())
        }
    }

    // Progress
    suspend fun savedPosition(id: String): Long = progressDao.getPosition(id) ?: 0L

    suspend fun saveProgress(item: MediaItem, positionMs: Long, durationMs: Long) {
        if (positionMs <= 0) return
        // Do not persist near-finished playback
        if (durationMs > 0 && positionMs > durationMs * 0.95) {
            progressDao.clear(item.id)
            return
        }
        progressDao.upsert(
            ProgressEntity(
                id = item.id,
                title = item.title,
                url = item.url,
                type = item.type.name,
                posterUrl = item.posterUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun FavoriteEntity.toMediaItem() = MediaItem(
        id = id, title = title, url = url,
        type = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.LIVE_CHANNEL),
        posterUrl = posterUrl, group = group
    )

    private fun ProgressEntity.toMediaItem() = MediaItem(
        id = id, title = title, url = url,
        type = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.MOVIE),
        posterUrl = posterUrl, group = "Continuar viendo"
    )

    private fun MediaItem.toFavoriteEntity() = FavoriteEntity(
        id = id, title = title, url = url, type = type.name,
        posterUrl = posterUrl, group = group
    )
}
