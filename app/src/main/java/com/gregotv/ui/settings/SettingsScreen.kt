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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.CrashReporter
import com.gregotv.ui.theme.GregoTvTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var newIptv by remember { mutableStateOf("") }
    var newSmb by remember { mutableStateOf("") }
    var xtHost by remember { mutableStateOf("") }
    var xtPort by remember { mutableStateOf("") }
    var xtUser by remember { mutableStateOf("") }
    var xtPass by remember { mutableStateOf("") }
    var crashText by remember { mutableStateOf<String?>(null) }
    var crashShown by remember { mutableStateOf(false) }

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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Listas IPTV",
                    color = GregoTvTheme.TextWhite,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Pega una URL m3u/m3u8. Sus canales se reparten solos por " +
                        "género (Deportes, Noticias, Películas...) y lo que no " +
                        "encaje aparece en «Mis fuentes».",
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
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
                    if (viewModel.addIptv(newIptv)) newIptv = ""
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

        // Xtream Codes section. Generic: no host, account or default is
        // bundled with the app — the user supplies their own provider.
        item {
            Text(
                "Fuentes Xtream Codes",
                color = GregoTvTheme.TextWhite,
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = xtHost,
                        onValueChange = { xtHost = it },
                        label = { androidx.compose.material3.Text("Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.55f)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = xtPort,
                        onValueChange = { xtPort = it },
                        label = { androidx.compose.material3.Text("Puerto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.4f)
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = xtUser,
                        onValueChange = { xtUser = it },
                        label = { androidx.compose.material3.Text("Usuario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.32f)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = xtPass,
                        onValueChange = { xtPass = it },
                        label = { androidx.compose.material3.Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(0.45f)
                    )
                    Button(onClick = {
                        if (viewModel.addXtream(xtHost, xtPort, xtUser, xtPass)) {
                            xtHost = ""; xtPort = ""; xtUser = ""; xtPass = ""
                        }
                    }) { Text("Añadir") }
                }
            }
        }
        items(settings.xtreamSources, key = { it.id }) { source ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    // Never render the password back to the screen.
                    text = "${source.username} @ ${source.host}:${source.port}",
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Button(onClick = { viewModel.removeXtream(source) }) { Text("Quitar") }
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

        // Diagnostics: read the last crash on the box itself, no adb needed.
        item {
            Text(
                "Diagnóstico",
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
                Button(onClick = {
                    crashText = CrashReporter.lastCrash(context)
                    crashShown = true
                }) { Text("Ver último error") }
                if (crashShown) {
                    Button(onClick = {
                        CrashReporter.clear(context)
                        crashText = null
                        crashShown = false
                    }) { Text("Borrar") }
                }
            }
        }
        if (crashShown) {
            item {
                Text(
                    text = crashText ?: "No hay ningún error registrado.",
                    color = GregoTvTheme.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}
