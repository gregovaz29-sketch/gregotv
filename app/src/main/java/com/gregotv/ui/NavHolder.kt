package com.gregotv.ui

import com.gregotv.model.MediaItem

/**
 * Lightweight in-memory hand-off for the currently selected item. Avoids
 * serializing MediaItem through nav args. Set before navigating to detail/player.
 */
object NavHolder {
    @Volatile
    var selected: MediaItem? = null
}

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
}
