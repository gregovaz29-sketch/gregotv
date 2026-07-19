package com.gregotv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.model.MediaItem
import com.gregotv.ui.components.ContentRow
import com.gregotv.ui.components.EmptyOrError
import com.gregotv.ui.components.HeroBanner
import com.gregotv.ui.components.HomeSkeleton
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun HomeScreen(
    onItemClick: (MediaItem) -> Unit,
    onOpenSettings: () -> Unit,
    mediaPermissionGranted: Boolean = true,
    onRequestPermission: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> HomeSkeleton()
            state.rows.isEmpty() ->
                EmptyOrError(state.error ?: "No hay contenido") { viewModel.load() }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item {
                    Box(Modifier.fillMaxWidth().height(320.dp)) {
                        state.hero?.let { HeroBanner(it) }
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                        ) {
                            Text("Ajustes")
                        }
                    }
                }
                if (!mediaPermissionGranted) {
                    item { LocalPermissionNotice(onRequestPermission) }
                }
                items(state.rows, key = { it.title }) { row ->
                    ContentRow(row = row, onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
private fun LocalPermissionNotice(onRequestPermission: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GregoTvTheme.DarkGray)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Concede el permiso de medios para ver tus vídeos locales. " +
                "IPTV y SMB funcionan sin él.",
            color = GregoTvTheme.TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Button(onClick = onRequestPermission) {
            Text("Conceder permiso")
        }
    }
}
