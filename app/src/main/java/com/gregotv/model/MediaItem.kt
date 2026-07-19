package com.gregotv.model

enum class MediaType {
    LOCAL,
    SMB,
    LIVE_CHANNEL,
    MOVIE,
    SERIES
}

data class MediaItem(
    val id: String,
    val title: String,
    val url: String,
    val type: MediaType,
    val posterUrl: String? = null,
    val group: String? = null,
    val description: String? = null
)

data class ContentRowData(
    val title: String,
    val items: List<MediaItem>
)
