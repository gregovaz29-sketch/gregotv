package com.gregotv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Surface
import com.gregotv.ui.NavHolder
import com.gregotv.ui.Routes
import com.gregotv.ui.browse.CategoryScreen
import com.gregotv.ui.browse.SearchScreen
import com.gregotv.ui.detail.DetailScreen
import com.gregotv.ui.home.HomeScreen
import com.gregotv.ui.home.HomeViewModel
import com.gregotv.ui.player.PlayerScreen
import com.gregotv.ui.settings.SettingsScreen
import com.gregotv.ui.theme.GregoTvTheme
import dagger.hilt.android.AndroidEntryPoint

/** Media permissions needed for the local library, by SDK level. */
private val mediaPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasMediaPermission(context: android.content.Context): Boolean =
    mediaPermissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            GregoTvTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GregoTvTheme.Black)
                ) {
                    GregoNavGraph()
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun GregoNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val context = LocalContext.current
            val homeVm: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()

            var granted by remember { mutableStateOf(hasMediaPermission(context)) }
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                val ok = result.values.any { it } || hasMediaPermission(context)
                granted = ok
                if (ok) homeVm.load() // re-scan local files once granted
            }

            // Ask once at startup. IPTV and SMB work regardless of the outcome.
            LaunchedEffect(Unit) {
                if (!granted) launcher.launch(mediaPermissions)
            }

            HomeScreen(
                viewModel = homeVm,
                mediaPermissionGranted = granted,
                onRequestPermission = { launcher.launch(mediaPermissions) },
                onItemClick = { item ->
                    NavHolder.selected = item
                    nav.navigate(Routes.DETAIL)
                },
                // Billboard "Reproducir" skips the detail screen.
                onPlay = { item ->
                    NavHolder.selected = item
                    nav.navigate(Routes.PLAYER)
                },
                onOpenCategory = { row ->
                    NavHolder.category = row
                    nav.navigate(Routes.CATEGORY)
                },
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CATEGORY) {
            val category = NavHolder.category
            if (category == null) {
                nav.popBackStack()
            } else {
                CategoryScreen(
                    category = category,
                    onItemClick = { item ->
                        NavHolder.selected = item
                        nav.navigate(Routes.DETAIL)
                    }
                )
            }
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onItemClick = { item ->
                    NavHolder.selected = item
                    nav.navigate(Routes.DETAIL)
                }
            )
        }
        composable(Routes.DETAIL) {
            val item = NavHolder.selected
            if (item == null) {
                nav.popBackStack()
            } else {
                DetailScreen(
                    item = item,
                    onPlay = { nav.navigate(Routes.PLAYER) }
                )
            }
        }
        composable(Routes.PLAYER) {
            val item = NavHolder.selected
            if (item == null) {
                nav.popBackStack()
            } else {
                PlayerScreen(item = item, onBack = { nav.popBackStack() })
            }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
