package com.gregotv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var newIptv by remember { mutableStateOf("") }
    var newSmb by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "Ajustes",
                color = GregoTvTheme.TextWhite,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Spanish-only toggle: applies to the default lists; user-added
        // sources always show regardless.
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Solo canales en español",
                    color = GregoTvTheme.TextWhite,
                    style = MaterialTheme.typography.titleMedium
                )
                androidx.compose.material3.Switch(
                    checked = settings.spanishOnly,
                    onCheckedChange = { viewModel.setSpanishOnly(it) }
                )
            }
        }

        // Adult toggle
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Contenido para adultos",
                    color = GregoTvTheme.TextWhite,
                    style = MaterialTheme.typography.titleMedium
                )
                androidx.compose.material3.Switch(
                    checked = settings.adultEnabled,
                    onCheckedChange = { viewModel.setAdult(it) }
                )
            }
        }

        // IPTV section
        item {
            Text(
                "Listas IPTV",
                color = GregoTvTheme.TextWhite,
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = newIptv,
                    onValueChange = { newIptv = it },
                    label = { androidx.compose.material3.Text("URL m3u/m3u8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Button(onClick = {
                    viewModel.addIptv(newIptv)
                    newIptv = ""
                }) { Text("Añadir") }
            }
        }
        items(settings.iptvUrls, key = { it }) { url ->
            val userAdded = url in settings.userIptvUrls
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (userAdded) "$url  · mía" else url,
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Button(onClick = { viewModel.removeIptv(url) }) { Text("Quitar") }
            }
        }

        // SMB section
        item {
            Text(
                "Rutas SMB",
                color = GregoTvTheme.TextWhite,
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = newSmb,
                    onValueChange = { newSmb = it },
                    label = { androidx.compose.material3.Text("smb://host/share/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Button(onClick = {
                    viewModel.addSmb(newSmb)
                    newSmb = ""
                }) { Text("Añadir") }
            }
        }
        items(settings.smbPaths, key = { it }) { path ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    path,
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Button(onClick = { viewModel.removeSmb(path) }) { Text("Quitar") }
            }
        }
    }
}
