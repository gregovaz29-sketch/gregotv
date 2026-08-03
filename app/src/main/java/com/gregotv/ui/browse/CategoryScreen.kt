package com.gregotv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem
import com.gregotv.ui.components.ContentCard
import com.gregotv.ui.theme.GregoTvTheme

/** One category opened as a full grid of channels. */
@Composable
fun CategoryScreen(
    category: ContentRowData,
    onItemClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = category.title,
            color = GregoTvTheme.TextWhite,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 48.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(200.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.items, key = { it.id }) { item ->
                ContentCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}
