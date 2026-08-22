package com.gregotv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.gregotv.ui.theme.GregoTvTheme

/** Home sections. Search and Settings navigate away instead of filtering. */
enum class HomeSection(val label: String) {
    INICIO("Inicio"),
    CANALES("Canales"),
    PELICULAS("Películas"),
    SERIES("Series")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNav(
    selected: HomeSection,
    onSelect: (HomeSection) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "GregoTV",
            color = GregoTvTheme.Red,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(end = 24.dp)
        )
        HomeSection.entries.forEach { section ->
            NavItem(
                label = section.label,
                active = section == selected,
                onClick = { onSelect(section) }
            )
        }
        Box(Modifier.weight(1f))
        NavItem(label = "Buscar", active = false, onClick = onSearch)
        NavItem(label = "Ajustes", active = false, onClick = onSettings)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    // Underline grows on focus; the label itself brightens when focused or
    // when it is the active section.
    val underline by animateFloatAsState(
        targetValue = if (focused) 1f else if (active) 0.6f else 0f,
        animationSpec = tween(180),
        label = "nav-underline"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.10f)
        )
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = if (focused || active) GregoTvTheme.TextWhite
                else GregoTvTheme.TextMuted,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
            if (underline > 0f) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width((44 * underline).dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GregoTvTheme.Red)
                )
            }
        }
    }
}
