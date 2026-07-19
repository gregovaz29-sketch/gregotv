package com.gregotv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gregotv.R
import com.gregotv.model.MediaItem
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun DetailScreen(
    item: MediaItem,
    onPlay: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit
) {
    var isFav by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.placeholder_poster),
            error = painterResource(R.drawable.placeholder_poster),
            fallback = painterResource(R.drawable.placeholder_poster),
            modifier = Modifier.fillMaxWidth().height(360.dp)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GregoTvTheme.Black)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            item.group?.let {
                Text(it, color = GregoTvTheme.TextMuted, style = MaterialTheme.typography.bodyLarge)
            }
            item.description?.let {
                Text(it, color = GregoTvTheme.TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onPlay) { Text("Reproducir") }
                Button(onClick = {
                    isFav = !isFav
                    onToggleFavorite(item)
                }) {
                    Text(if (isFav) "Quitar de favoritos" else "Añadir a favoritos")
                }
            }
        }
    }
}
