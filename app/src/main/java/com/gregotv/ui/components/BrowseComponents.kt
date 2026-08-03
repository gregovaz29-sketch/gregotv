package com.gregotv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.model.ContentRowData
import com.gregotv.ui.theme.GregoTvTheme

/** Emoji shown on a category folder, by category title. */
private fun folderIcon(title: String): String = when (title) {
    "Deportes" -> "⚽"
    "Noticias" -> "📰"
    "Películas", "Mis películas" -> "🎬"
    "Series", "Mis series" -> "📺"
    "Infantil" -> "🧸"
    "Documentales" -> "🌍"
    "Música" -> "🎵"
    "Entretenimiento" -> "🎭"
    "Cultura y educación" -> "📚"
    "Estilo de vida" -> "🍳"
    "Autonómicas" -> "📍"
    "Religión" -> "⛪"
    "España" -> "🇪🇸"
    "En español" -> "🗣️"
    "Latinoamérica" -> "🌎"
    "Internacional" -> "🌐"
    "Red / SMB" -> "🖧"
    "Continuar viendo" -> "▶️"
    "Mis favoritos" -> "⭐"
    else -> "📁"
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderCard(
    row: ContentRowData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        colors = CardDefaults.colors(containerColor = GregoTvTheme.DarkGray),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, GregoTvTheme.Red),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = folderIcon(row.title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = row.title,
                    color = GregoTvTheme.TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${row.items.size} canales",
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
