package com.codershubinc.aaxion_mob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import com.codershubinc.aaxion_mob.auth.TokenManager
import com.codershubinc.aaxion_mob.ui.AaxionDriveScreen
import com.codershubinc.aaxion_mob.ui.LoginScreen
import com.codershubinc.aaxion_mob.ui.theme.AaxionMobTheme
import com.codershubinc.aaxion_mob.ui.theme.AaxionPrimary
import com.codershubinc.aaxion_mob.utils.DataStoreManager
import com.codershubinc.aaxion_mob.utils.ServerDiscovery
import com.codershubinc.aaxion_mob.viewmodels.FileViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateSystemBars()
        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.1)
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
        setContent {
            val context = LocalContext.current
            val dataStoreManager = remember { DataStoreManager(context) }
            val savedAccentId by dataStoreManager.accentTheme.collectAsState(initial = null)
            val savedIconPackId by dataStoreManager.iconPackTheme.collectAsState(initial = null)

            val activeAccent = remember(savedAccentId) {
                com.codershubinc.aaxion_mob.models.AccentTheme.fromId(savedAccentId)
            }
            val activeIconPack = remember(savedIconPackId) {
                com.codershubinc.aaxion_mob.models.IconPackTheme.fromId(savedIconPackId)
            }

            AaxionMobTheme(
                accentTheme = activeAccent,
                iconPackTheme = activeIconPack
            ) {
                AppNavigation()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateSystemBars()
        }
    }

    private fun updateSystemBars() {
        val configuration = resources.configuration
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isTablet = configuration.smallestScreenWidthDp >= 600

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isLandscape || isTablet) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dataStoreManager = remember { DataStoreManager(context) }
    val tokenManager = remember { TokenManager.getInstance(context) }
    val fileViewModel: FileViewModel = viewModel()

    val savedMasterToken by dataStoreManager.masterToken.collectAsState(initial = null)
    val savedAccessToken by dataStoreManager.accessToken.collectAsState(initial = null)
    val savedServerUri by dataStoreManager.serverUri.collectAsState(initial = null)
    val prioritizedServerUri by dataStoreManager.prioritizedServerUri.collectAsState(initial = null)

    val discoveredServerUris = remember { mutableStateListOf<String>() }
    var discoveredServerUri by remember { mutableStateOf("") }

    // Start discovery and load initial tokens
    DisposableEffect(Unit) {
        val discovery = ServerDiscovery(context) { discoveredIp, port ->
            val uri = "http://$discoveredIp:$port"
            discoveredServerUri = uri
            if (!discoveredServerUris.contains(uri)) {
                discoveredServerUris.add(uri)
            }
        }
        discovery.startDiscovery()

        scope.launch {
            tokenManager.loadInitialTokens()
        }

        onDispose {
            discovery.stopDiscovery()
        }
    }

    val activeServerUri = remember(prioritizedServerUri, discoveredServerUri, savedServerUri) {
        when {
            !prioritizedServerUri.isNullOrBlank() -> prioritizedServerUri!!
            discoveredServerUri.isNotBlank() -> discoveredServerUri
            !savedServerUri.isNullOrBlank() -> savedServerUri!!
            else -> ""
        }
    }

    // Start 4-hour automatic access token refresh scheduler
    LaunchedEffect(activeServerUri) {
        if (activeServerUri.isNotBlank()) {
            tokenManager.startPeriodicRefresh(scope) { activeServerUri }
        }
    }

    val isTokenManagerInitialized by tokenManager.isInitialized.collectAsState()
    val activeMasterToken by tokenManager.masterTokenState.collectAsState()
    val activeAccessToken by tokenManager.accessTokenState.collectAsState()

    // Seamless AMOLED boot state: Don't render until tokens are checked from disk
    if (!isTokenManagerInitialized) {
        AaxionSplashScreen()
        return
    }

    val isLoggedIn = !activeAccessToken.isNullOrBlank() && !activeMasterToken.isNullOrBlank()
    val startDestination = if (isLoggedIn) "drive" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                serverUri = activeServerUri,
                onLoginSuccess = {
                    navController.navigate("drive") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("drive") {
            AaxionDriveScreen(
                serverUri = activeServerUri,
                token = activeAccessToken ?: "",
                viewModel = fileViewModel,
                discoveredUris = discoveredServerUris.toList(),
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("drive") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Backward-compatibility aliases
        composable("me") {
            AaxionDriveScreen(
                serverUri = activeServerUri,
                token = activeAccessToken ?: "",
                viewModel = fileViewModel,
                discoveredUris = discoveredServerUris.toList(),
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("me") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("explorer") {
            AaxionDriveScreen(
                serverUri = activeServerUri,
                token = activeAccessToken ?: "",
                viewModel = fileViewModel,
                discoveredUris = discoveredServerUris.toList(),
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("explorer") { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AaxionSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AaxionPrimary.copy(alpha = 0.12f * alpha))
                    .border(
                        width = 1.5.dp,
                        color = AaxionPrimary.copy(alpha = 0.6f * alpha),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = AaxionPrimary.copy(alpha = alpha),
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "AAXION",
                color = Color.White.copy(alpha = alpha),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
        }
    }
}

