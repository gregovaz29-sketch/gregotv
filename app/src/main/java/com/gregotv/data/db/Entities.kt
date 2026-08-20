package com.gregotv.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val type: String,
    val posterUrl: String?,
    val group: String?
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val type: String,
    val posterUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

/**
 * A record of a live channel URL failing to play. The player writes here on
 * error; the loader hides recent failures so a dead channel does not keep
 * showing up. Re-checked after [ChannelHealthDao.QUARANTINE_MS] has elapsed.
 */
@Entity(tableName = "channel_health")
data class ChannelHealthEntity(
    /** Stream URL; matches [MediaItem.url] for live channels. */
    @PrimaryKey val url: String,
    /** "dead" | "ok". Only "dead" hides the channel. */
    val status: String,
    val lastCheckedAt: Long
)
