package com.gregotv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.R
import com.gregotv.model.MediaItem
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun HeroBanner(item: MediaItem) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
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
            modifier = Modifier.fillMaxWidth().height(320.dp)
        )
        // Left-to-right + bottom scrim for readable text
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(GregoTvTheme.Black, Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GregoTvTheme.Black)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 32.dp, end = 200.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.group?.let {
                Text(
                    text = it,
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
