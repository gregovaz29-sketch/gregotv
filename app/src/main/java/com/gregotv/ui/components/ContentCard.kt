package com.gregotv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.gregotv.model.MediaItem
import com.gregotv.model.MediaType
import com.gregotv.ui.theme.GregoTvTheme

/** Stable per-title tint so logo-less cards are distinguishable, not grey mush. */
private val TINTS = listOf(
    Color(0xFF3A2B4D), Color(0xFF1F3A4D), Color(0xFF4D3A2B),
    Color(0xFF2B4D3A), Color(0xFF4D2B33), Color(0xFF2B334D)
)

private fun tintFor(title: String): Color =
    TINTS[(title.hashCode().let { if (it == Int.MIN_VALUE) 0 else it } % TINTS.size + TINTS.size) % TINTS.size]

/** First one or two meaningful characters, used when there is no logo. */
private fun initialsOf(title: String): String {
    val words = title.split(' ', '-', '_')
        .filter { it.isNotBlank() && it.first().isLetterOrDigit() }
    return when {
        words.isEmpty() -> "TV"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    // The title strip fades in on focus so unfocused rows stay clean.
    val labelAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0.82f,
        animationSpec = tween(160),
        label = "card-label"
    )
    var imageFailed by remember(item.id) { mutableStateOf(item.posterUrl == null) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(200.dp)
            .height(112.dp)
            .onFocusChanged { focused = it.isFocused },
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(focusedScale = 1.12f),
        colors = CardDefaults.colors(containerColor = tintFor(item.title)),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, GregoTvTheme.Red),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            // Fallback layer: always drawn, revealed when there is no artwork.
            // Beats a generic broken-image placeholder on a wall of channels.
            if (imageFailed) {
                Box(
                    Modifier.fillMaxSize().background(tintFor(item.title)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsOf(item.title),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) imageFailed = true
                    },
                    modifier = Modifier.fillMaxSize().padding(10.dp)
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )
            Text(
                text = item.title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .alpha(labelAlpha)
            )
            if (item.type == MediaType.LIVE_CHANNEL) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            GregoTvTheme.Red.copy(alpha = 0.92f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "EN VIVO",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
