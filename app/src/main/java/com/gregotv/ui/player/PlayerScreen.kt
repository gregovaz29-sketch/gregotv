package com.gregotv.ui.player

import android.view.KeyEvent
import android.view.LayoutInflater
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.gregotv.R
import com.gregotv.model.MediaItem
import com.gregotv.ui.theme.GregoTvTheme

/** Plenty of IPTV hosts reject the default agent or bounce http -> https. */
private const val USER_AGENT = "GregoTV/1.0 (Android TV; Media3)"

@UnstableApi
@Composable
fun PlayerScreen(
    item: MediaItem,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var notice by remember { mutableStateOf<String?>(null) }

    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        // Wrapped so file:// and content:// sources (local library, SMB cache)
        // keep working alongside the HTTP streams.
        val sources = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http))

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(sources)
            .build()
            .apply {
                setMediaItem(ExoMediaItem.fromUri(item.url))
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(player) {
        val handler = Handler(Looper.getMainLooper())
        var successReported = false
        val reportStablePlayback = Runnable {
            if (item.type == com.gregotv.model.MediaType.LIVE_CHANNEL &&
                player.playbackState == Player.STATE_READY && player.isPlaying
            ) {
                successReported = true
                viewModel.reportSuccess(item.url)
            }
        }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                notice = "No se puede reproducir (${error.errorCodeName})"
                // Record the failure only for live channels; local/SMB errors
                // are usually transient and don't warrant hiding the entry.
                if (item.type == com.gregotv.model.MediaType.LIVE_CHANNEL) {
                    handler.removeCallbacks(reportStablePlayback)
                    viewModel.reportFailure(item.url)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (successReported || item.type != com.gregotv.model.MediaType.LIVE_CHANNEL) return
                if (isPlaying) handler.postDelayed(reportStablePlayback, 10_000)
                else handler.removeCallbacks(reportStablePlayback)
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (tracks.groups.isEmpty()) return
                val hasVideo = tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO }
                val videoSupported = tracks.groups.any {
                    it.type == C.TRACK_TYPE_VIDEO && it.isSupported
                }
                notice = when {
                    !hasVideo -> "Este canal emite solo audio"
                    !videoSupported -> "El vídeo usa un códec que la TV no soporta"
                    else -> null
                }
            }
        }
        player.addListener(listener)
        onDispose {
            handler.removeCallbacks(reportStablePlayback)
            player.removeListener(listener)
            viewModel.save(item, player.currentPosition, player.duration)
            player.release()
        }
    }

    // Resume from saved position (live channels always start at 0).
    LaunchedEffect(item.id) {
        val pos = viewModel.startPosition(item.id)
        if (pos > 0) player.seekTo(pos)
    }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx)
                    .inflate(R.layout.view_player, null) as PlayerView
                view.player = player
                view.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        // save() already launches a guarded coroutine; call it
                        // directly rather than wrapping it in another scope.
                        viewModel.save(item, player.currentPosition, player.duration)
                        onBack()
                        true
                    } else false
                }
                view
            }
        )

        notice?.let { message ->
            Text(
                text = message,
                color = GregoTvTheme.TextWhite,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .background(GregoTvTheme.DarkGray)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
