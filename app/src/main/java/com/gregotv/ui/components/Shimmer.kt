package com.gregotv.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val brush = Brush.horizontalGradient(
        listOf(
            GregoTvTheme.DarkGray.copy(alpha = alpha),
            Color(0xFF3A3A3A).copy(alpha = alpha),
            GregoTvTheme.DarkGray.copy(alpha = alpha)
        )
    )
    Spacer(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(brush))
}

@Composable
fun HomeSkeleton() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ShimmerBox(Modifier.height(280.dp).width(760.dp))
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(5) {
                    ShimmerBox(Modifier.size(width = 200.dp, height = 112.dp))
                }
            }
        }
    }
}
