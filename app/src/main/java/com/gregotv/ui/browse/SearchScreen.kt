package com.gregotv.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.model.MediaItem
import com.gregotv.ui.NavHolder
import com.gregotv.ui.components.ContentCard
import com.gregotv.ui.theme.GregoTvTheme
import java.text.Normalizer

private fun fold(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .lowercase()

@Composable
fun SearchScreen(onItemClick: (MediaItem) -> Unit) {
    var query by remember { mutableStateOf("") }

    // Scan every category once; dedupe channels shown in more than one folder.
    val allItems = remember {
        NavHolder.rows.flatMap { it.items }.distinctBy { it.id }
    }
    val results = remember(query) {
        val q = fold(query.trim())
        if (q.length < 2) emptyList()
        else allItems.filter { fold(it.title).contains(q) }
            .sortedWith(compareByDescending<MediaItem> { it.spanish }.thenBy { it.title.lowercase() })
            .take(120)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 32.dp, start = 48.dp, end = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar canal") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            modifier = Modifier.fillMaxWidth()
        )

        when {
            query.trim().length < 2 -> Text(
                "Escribe al menos 2 letras",
                color = GregoTvTheme.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            results.isEmpty() -> Text(
                "Sin resultados para \"$query\"",
                color = GregoTvTheme.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(200.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    ContentCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}
