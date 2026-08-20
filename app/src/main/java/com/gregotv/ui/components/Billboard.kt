package com.gregotv.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gregotv.R
import com.gregotv.model.MediaItem
import com.gregotv.ui.theme.GregoTvTheme
import kotlinx.coroutines.delay

private const val ROTATE_MS = 9_000L

/**
 * Netflix-style billboard: full-bleed art, scrims for legibility, title and
 * actions. Rotates through [items] on a timer, pausing while the user has
 * focus on one of the buttons so the target never moves under the D-pad.
 */
@Composable
fun Billboard(
    items: List<MediaItem>,
    isFavorite: (MediaItem) -> Boolean,
    onPlay: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onInfo: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var index by remember(items) { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(items, paused) {
        if (items.size <= 1 || paused) return@LaunchedEffect
        while (true) {
            delay(ROTATE_MS)
            index = (index + 1) % items.size
        }
    }

    val item = items[index.coerceIn(items.indices)]

    Box(modifier.fillMaxWidth().height(420.dp)) {
        AnimatedContent(
            targetState = item,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
            label = "billboard-art"
        ) { current ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(current.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = current.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.placeholder_poster),
                error = painterResource(R.drawable.placeholder_poster),
                fallback = painterResource(R.drawable.placeholder_poster),
                modifier = Modifier.fillMaxSize()
            )
        }
        // Left scrim keeps the text readable over any artwork; bottom scrim
        // blends the billboard into the first carousel.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to GregoTvTheme.Black,
                    0.55f to GregoTvTheme.Black.copy(alpha = 0.35f),
                    1f to Color.Transparent
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to GregoTvTheme.Black
                )
            )
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 36.dp, end = 260.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.group?.let {
                Text(
                    text = it,
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPlay(item) },
                    modifier = Modifier.onFocusChanged { paused = it.hasFocus }
                ) { Text("▶  Reproducir") }
                Button(
                    onClick = { onToggleFavorite(item) },
                    modifier = Modifier.onFocusChanged { paused = it.hasFocus }
                ) {
                    Text(if (isFavorite(item)) "✓  En mi lista" else "+  Mi lista")
                }
                Button(
                    onClick = { onInfo(item) },
                    modifier = Modifier.onFocusChanged { paused = it.hasFocus }
                ) { Text("Info") }
            }
        }

        // Rotation indicator, bottom-right.
        if (items.size > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEachIndexed { i, _ ->
                    Box(
                        Modifier
                            .size(if (i == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == index) GregoTvTheme.Red
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}
