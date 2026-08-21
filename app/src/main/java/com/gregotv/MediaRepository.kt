package com.gregotv

import com.gregotv.data.Genres
import com.gregotv.data.LocalScanner
import com.gregotv.data.M3uParser
import com.gregotv.data.SmbScanner
import com.gregotv.data.XtreamClient
import com.gregotv.data.db.ChannelHealthDao
import com.gregotv.data.db.ChannelHealthEntity
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
    private val xtreamClient: XtreamClient,
    private val smbScanner: SmbScanner,
    private val localScanner: LocalScanner,
    private val settingsRepo: SettingsRepository,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ProgressDao,
    private val channelHealthDao: ChannelHealthDao
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
        val xtreamDeferred = settings.xtreamSources.map { source ->
            async { xtreamClient.liveChannels(source) }
        }
        val smbDeferred = settings.smbPaths.map { path ->
            async { smbScanner.scan(path) }
        }
        val localDeferred = async { localScanner.scan() }

        // The default lists overlap heavily (es + spa + tdtchannels all carry
        // similar Spanish channels), so collapse duplicates across lists by
        // channel name.
        // Skip channels the player marked dead within the quarantine window;
        // house-keep older entries so they get another chance.
        val now = System.currentTimeMillis()
        channelHealthDao.purgeOlderThan(now - ChannelHealthDao.QUARANTINE_MS)
        val deadUrls = channelHealthDao
            .deadSince(now - ChannelHealthDao.QUARANTINE_MS)
            .toHashSet()

        // No language filter here on purpose. The catalogue is trimmed at the
        // source, by which lists get downloaded; `MediaItem.spanish` is a
        // heuristic with false negatives and is only used for ordering below.
        val iptv = (iptvDeferred.flatMap { it.await() } +
            xtreamDeferred.flatMap { it.await() })
            .distinctBy { Genres.channelKey(it.title).ifBlank { it.url } }
            .filterNot { it.url in deadUrls }
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

        // Local movies / series. Titles differ from the live genre rows below so
        // every row title stays unique (the home list keys rows by title).
        local.filter { it.type == MediaType.MOVIE }.takeIf { it.isNotEmpty() }?.let {
            rows += ContentRowData("Mis películas", it)
        }
        local.filter { it.type == MediaType.SERIES }.takeIf { it.isNotEmpty() }?.let {
            rows += ContentRowData("Mis series", it)
        }

        // SMB
        if (smb.isNotEmpty()) rows += ContentRowData("Red / SMB", smb)

        // Live channels: one row per unified genre, in a fixed order. Spanish
        // channels sort to the front of every row so the mostly-English global
        // list does not bury them.
        val byGenre = iptv.groupBy { Genres.of(it) }
        Genres.ORDER.forEach { genre ->
            val channels = byGenre[genre].orEmpty()
            if (channels.isNotEmpty()) {
                rows += ContentRowData(
                    genre,
                    channels
                        .sortedWith(
                            compareByDescending<MediaItem> { it.spanish }
                                .thenBy { it.title.lowercase() }
                        )
                        .take(MAX_CHANNELS_PER_ROW)
                )
            }
        }

        rows
    }

    /**
     * Billboard rotation: one pick per genre row so the featured strip is
     * varied rather than five channels from the same category. Prefers Spanish
     * channels that actually have artwork.
     */
    fun heroItems(rows: List<ContentRowData>, count: Int = 5): List<MediaItem> {
        val picks = rows
            .filter { it.title !in NON_FEATURED_ROWS }
            .mapNotNull { row ->
                row.items.firstOrNull { it.spanish && it.posterUrl != null }
                    ?: row.items.firstOrNull { it.posterUrl != null }
            }
            .distinctBy { it.id }
        return picks.take(count).ifEmpty {
            rows.flatMap { it.items }.take(1)
        }
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

    /** Called by the player on playback failure of a live channel. */
    suspend fun reportChannelFailure(url: String) {
        channelHealthDao.upsert(
            ChannelHealthEntity(
                url = url,
                status = "dead",
                lastCheckedAt = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        /** Rows that make poor billboard material (already-seen / user data). */
        val NON_FEATURED_ROWS = setOf(
            "Continuar viendo", "Mis favoritos", "Red / SMB"
        )

        /**
         * Cap per row. "Internacional" alone holds ~4000 channels after merging
         * and a D-pad cannot realistically cross that.
         */
        const val MAX_CHANNELS_PER_ROW = 200
    }
}
