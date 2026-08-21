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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val server by viewModel.server.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The upload server lives exactly as long as this screen is on screen:
    // started when it resumes, stopped when it stops. Never in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startServer()
                Lifecycle.Event.ON_STOP -> viewModel.stopServer()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopServer()
        }
    }

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

        // Add-from-phone card: browse to this address on a phone on the same
        // network and paste lists there instead of typing with the D-pad.
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Añadir desde el móvil",
                    color = GregoTvTheme.TextWhite,
                    style = MaterialTheme.typography.titleMedium
                )
                val info = server
                if (info?.ip != null) {
                    Text(
                        "En el navegador del móvil (misma red):",
                        color = GregoTvTheme.TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "http://${info.ip}:${info.port}",
                        color = GregoTvTheme.TextWhite,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "PIN: ${info.pin}",
                        color = GregoTvTheme.Red,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    Text(
                        "No se pudo iniciar el servidor local (¿sin red?). " +
                            "Puedes añadir listas escribiendo la URL abajo.",
                        color = GregoTvTheme.TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
