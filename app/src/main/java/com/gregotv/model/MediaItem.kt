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
    val description: String? = null,
    /** Where the entry came from, used as the last resort when no genre matches. */
    val origin: String? = null,
    /** True when the channel is Spanish-language; used to sort it to the front. */
    val spanish: Boolean = false,
    /**
     * ISO country code, when known. Part of the dedupe key so that channels
     * sharing a name across countries (TVN in Chile, Panama and the Dominican
     * Republic) stop collapsing into one another.
     */
    val country: String? = null
)

data class ContentRowData(
    val title: String,
    val items: List<MediaItem>
)
