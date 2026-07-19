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
