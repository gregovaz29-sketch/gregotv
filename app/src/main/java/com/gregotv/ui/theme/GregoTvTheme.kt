package com.gregotv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object GregoTvTheme {
    val Red = Color(0xFFE50914)
    val Black = Color(0xFF141414)
    val DarkGray = Color(0xFF2A2A2A)
    val TextWhite = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFB3B3B3)
}

private val GregoColorScheme = darkColorScheme(
    primary = GregoTvTheme.Red,
    onPrimary = GregoTvTheme.TextWhite,
    background = GregoTvTheme.Black,
    onBackground = GregoTvTheme.TextWhite,
    surface = GregoTvTheme.DarkGray,
    onSurface = GregoTvTheme.TextWhite
)

@Composable
fun GregoTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GregoColorScheme,
        content = content
    )
}
