package com.gregotv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gregotv.R
import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun DetailScreen(
    item: MediaItem,
    onPlay: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val isFav by viewModel.isFavorite.collectAsStateWithLifecycle()
    LaunchedEffect(item.id) { viewModel.observe(item) }

    Box(Modifier.fillMaxSize().background(GregoTvTheme.Black)) {
        // Full-bleed art behind everything, faded so the copy stays readable.
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
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to GregoTvTheme.Black,
                    0.6f to GregoTvTheme.Black.copy(alpha = 0.7f),
                    1f to GregoTvTheme.Black.copy(alpha = 0.25f)
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to GregoTvTheme.Black.copy(alpha = 0.5f),
                    0.55f to Color.Transparent,
                    1f to GregoTvTheme.Black
                )
            )
        )

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.6f)
                .padding(start = 56.dp, top = 72.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (item.type == MediaType.LIVE_CHANNEL) {
                Box(
                    Modifier
                        .background(GregoTvTheme.Red, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "EN VIVO",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            item.group?.let {
                Text(
                    text = it,
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item.description?.let {
                Text(
                    text = it,
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Button(onClick = onPlay) { Text("▶  Reproducir") }
                Button(onClick = { viewModel.toggleFavorite(item) }) {
                    Text(if (isFav) "✓  En mi lista" else "+  Mi lista")
                }
            }
        }
    }
}
