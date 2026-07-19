package com.gregotv.ui.player

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.gregotv.model.MediaItem
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun PlayerScreen(
    item: MediaItem,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.url))
            playWhenReady = true
            prepare()
        }
    }

    // Resume from saved position (live channels always start at 0).
    LaunchedEffect(item.id) {
        val pos = viewModel.startPosition(item.id)
        if (pos > 0) player.seekTo(pos)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.save(item, player.currentPosition, player.duration)
            player.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_UP &&
                            keyCode == KeyEvent.KEYCODE_BACK
                        ) {
                            scope.launch {
                                viewModel.save(item, player.currentPosition, player.duration)
                            }
                            onBack()
                            true
                        } else false
                    }
                }
            }
        )
    }
}
