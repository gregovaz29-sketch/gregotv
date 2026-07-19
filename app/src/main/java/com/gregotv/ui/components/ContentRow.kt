package com.gregotv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.model.ContentRowData
import com.gregotv.model.MediaItem
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun ContentRow(
    row: ContentRowData,
    onItemClick: (MediaItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = row.title,
            color = GregoTvTheme.TextWhite,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 48.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(row.items, key = { it.id }) { item ->
                ContentCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}
