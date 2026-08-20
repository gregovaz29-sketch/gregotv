package com.gregotv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem
import com.gregotv.ui.NavHolder
import com.gregotv.ui.components.Billboard
import com.gregotv.ui.components.ContentRow
import com.gregotv.ui.components.EmptyOrError
import com.gregotv.ui.components.FolderCard
import com.gregotv.ui.components.HomeSection
import com.gregotv.ui.components.HomeSkeleton
import com.gregotv.ui.components.TopNav
import com.gregotv.ui.theme.GregoTvTheme

/** Rows shown as carousels on Inicio; everything else becomes a folder. */
private val QUICK_ROWS = listOf("Continuar viendo", "Mis favoritos")

/** Rows that are not live-channel genres. */
private val NON_CHANNEL_ROWS =
    QUICK_ROWS + listOf("Mis películas", "Mis series", "Red / SMB")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onItemClick: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onOpenCategory: (ContentRowData) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    mediaPermissionGranted: Boolean = true,
    onRequestPermission: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(HomeSection.INICIO) }

    // Keep the full category list reachable by the search screen.
    NavHolder.rows = state.rows

    Column(Modifier.fillMaxSize()) {
        TopNav(
            selected = section,
            onSelect = { section = it },
            onSearch = onOpenSearch,
            onSettings = onOpenSettings
        )

        when {
            state.loading -> HomeSkeleton()
            state.rows.isEmpty() ->
                EmptyOrError(state.error ?: "No hay contenido") { viewModel.load() }
            else -> SectionContent(
                section = section,
                state = state,
                onItemClick = onItemClick,
                onPlay = onPlay,
                onOpenCategory = onOpenCategory,
                onToggleFavorite = viewModel::toggleFavorite,
                mediaPermissionGranted = mediaPermissionGranted,
                onRequestPermission = onRequestPermission
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionContent(
    section: HomeSection,
    state: HomeState,
    onItemClick: (MediaItem) -> Unit,
    onPlay: (MediaItem) -> Unit,
    onOpenCategory: (ContentRowData) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    mediaPermissionGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    val rows = state.rows
    val quick = rows.filter { it.title in QUICK_ROWS }
    val channelRows = rows.filter { it.title !in NON_CHANNEL_ROWS }

    // Each section picks the rows it cares about; Inicio keeps the billboard.
    val carousels: List<ContentRowData>
    val folders: List<ContentRowData>
    when (section) {
        HomeSection.INICIO -> {
            carousels = quick
            folders = rows.filter { it.title !in QUICK_ROWS }
        }
        HomeSection.CANALES -> {
            carousels = emptyList()
            folders = channelRows
        }
        HomeSection.PELICULAS -> {
            carousels = rows.filter { it.title == "Mis películas" || it.title == "Películas" }
            folders = emptyList()
        }
        HomeSection.SERIES -> {
            carousels = rows.filter { it.title == "Mis series" || it.title == "Series" }
            folders = emptyList()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        if (section == HomeSection.INICIO && state.heroes.isNotEmpty()) {
            item(key = "billboard") {
                Billboard(
                    items = state.heroes,
                    isFavorite = { it.id in state.favoriteIds },
                    onPlay = onPlay,
                    onToggleFavorite = onToggleFavorite,
                    onInfo = onItemClick
                )
            }
        }

        if (!mediaPermissionGranted && section == HomeSection.INICIO) {
            item(key = "permission") { LocalPermissionNotice(onRequestPermission) }
        }

        items(carousels, key = { "row-${it.title}" }) { row ->
            ContentRow(row = row, onItemClick = onItemClick)
        }

        if (folders.isNotEmpty()) {
            item(key = "folders-header") {
                Text(
                    text = if (section == HomeSection.CANALES) "Todos los canales"
                    else "Categorías",
                    color = GregoTvTheme.TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
            item(key = "folders") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    folders.forEach { row ->
                        Box(Modifier.width(240.dp)) {
                            FolderCard(row = row, onClick = { onOpenCategory(row) })
                        }
                    }
                }
            }
        }

        if (carousels.isEmpty() && folders.isEmpty()) {
            item(key = "empty-section") {
                Text(
                    text = "Nada en esta sección todavía",
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 48.dp, top = 32.dp)
                )
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
