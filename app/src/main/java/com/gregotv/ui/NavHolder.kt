package com.gregotv.ui

import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem

/**
 * Lightweight in-memory hand-off between screens. Avoids serializing MediaItem
 * lists through nav args. Set before navigating.
 */
object NavHolder {
    /** Item opened in detail / player. */
    @Volatile
    var selected: MediaItem? = null

    /** Every category, used by the search screen to scan all channels. */
    @Volatile
    var rows: List<ContentRowData> = emptyList()

    /** Category folder opened in the category screen. */
    @Volatile
    var category: ContentRowData? = null
}

object Routes {
    const val HOME = "home"
    const val CATEGORY = "category"
    const val SEARCH = "search"
    const val DETAIL = "detail"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
}
