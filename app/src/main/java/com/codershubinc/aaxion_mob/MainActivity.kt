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
import okhttp3.OkHttpClient
import com.codershubinc.aaxion_mob.core.data.DataStoreManager
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.model.AccentTheme
import com.codershubinc.aaxion_mob.core.model.IconPackTheme
import com.codershubinc.aaxion_mob.core.network.ServerDiscovery
import com.codershubinc.aaxion_mob.feature.auth.LoginScreen
import com.codershubinc.aaxion_mob.feature.drive.AaxionDriveScreen
import com.codershubinc.aaxion_mob.feature.drive.FileViewModel
import com.codershubinc.aaxion_mob.feature.settings.ConnectionManagerScreen
import com.codershubinc.aaxion_mob.feature.settings.SettingsScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.codershubinc.aaxion_mob.ui.theme.AaxionMobTheme
import com.codershubinc.aaxion_mob.ui.theme.AaxionPrimary
import com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine

class MainActivity : ComponentActivity() {

    private val vlcVideoResultLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        val returnedPosition = intentData?.getLongExtra("extra_position", -1L)
            ?: intentData?.getLongExtra("position", -1L)
            ?: intentData?.getLongExtra("result_position", -1L)
            ?: -1L

        val returnedDuration = intentData?.getLongExtra("extra_duration", 0L)
            ?: intentData?.getLongExtra("duration", 0L)
            ?: 0L

        com.codershubinc.aaxion_mob.core.media.VlcPlayerManager.getInstance(applicationContext).handleActivityResult(
            positionMs = returnedPosition,
            durationMs = returnedDuration
        )
    }

    companion object {
        var isStatusBarHiddenCached: Boolean = false
            internal set
        var isNavBarHiddenCached: Boolean = false
            internal set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySystemBars()
        com.codershubinc.aaxion_mob.core.media.VlcPlayerManager.getInstance(applicationContext).setLauncher(vlcVideoResultLauncher)

        lifecycleScope.launch {
            val dataStoreManager = DataStoreManager(this@MainActivity)
            dataStoreManager.hideStatusBar.collect { hidden ->
                isStatusBarHiddenCached = hidden
                applySystemBars()
            }
        }
        lifecycleScope.launch {
            val dataStoreManager = DataStoreManager(this@MainActivity)
            dataStoreManager.hideNavigationBar.collect { hidden ->
                isNavBarHiddenCached = hidden
                applySystemBars()
            }
        }

        val coilOkHttpClient = com.codershubinc.aaxion_mob.core.network.Fetcher.getInstance().getClient().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val token = TokenManager.getInstance(applicationContext).accessTokenState.value
                val newRequest = if (!token.isNullOrBlank()) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
                chain.proceed(newRequest)
            }
            .build()

        val imageLoader = ImageLoader.Builder(this)
            .okHttpClient(coilOkHttpClient)
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
            val hideStatusBar by dataStoreManager.hideStatusBar.collectAsState(initial = isStatusBarHiddenCached)
            val hideNavBar by dataStoreManager.hideNavigationBar.collectAsState(initial = isNavBarHiddenCached)

            val window = (context as? ComponentActivity)?.window
            LaunchedEffect(hideStatusBar, hideNavBar, window) {
                isStatusBarHiddenCached = hideStatusBar
                isNavBarHiddenCached = hideNavBar
                applySystemBars()
            }

            val activeAccent = remember(savedAccentId) {
                AccentTheme.fromId(savedAccentId)
            }
            val activeIconPack = remember(savedIconPackId) {
                IconPackTheme.fromId(savedIconPackId)
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
            applySystemBars()
        }
    }

    private fun applySystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        var hideTypes = 0
        var showTypes = 0
        if (isStatusBarHiddenCached) {
            hideTypes = hideTypes or WindowInsetsCompat.Type.statusBars()
        } else {
            showTypes = showTypes or WindowInsetsCompat.Type.statusBars()
        }
        if (isNavBarHiddenCached) {
            hideTypes = hideTypes or WindowInsetsCompat.Type.navigationBars()
        } else {
            showTypes = showTypes or WindowInsetsCompat.Type.navigationBars()
        }

        if (hideTypes != 0) insetsController.hide(hideTypes)
        if (showTypes != 0) insetsController.show(showTypes)
    }

    override fun onResume() {
        super.onResume()
        com.codershubinc.aaxion_mob.core.media.VlcPlayerManager.getInstance(applicationContext).onActivityResumed()
    }

    override fun onDestroy() {
        super.onDestroy()
        com.codershubinc.aaxion_mob.core.media.VlcPlayerManager.getInstance(applicationContext).setLauncher(null)
        com.codershubinc.aaxion_mob.core.media.MediaPlayerManager.getInstance(applicationContext).release()
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
    val prioritizedServerUris by dataStoreManager.prioritizedServerUris.collectAsState(initial = emptyList())
    val savedTunnelUri by dataStoreManager.tunnelServerUri.collectAsState(initial = null)

    val discoveredServerUris = remember { mutableStateListOf<String>() }
    val refreshDiscovery = remember { mutableStateOf(0) }
    var discoveredServerUri by remember { mutableStateOf("") }

    // Start discovery and load initial tokens
    DisposableEffect(refreshDiscovery.value) {
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

    val activeServerUri = remember(prioritizedServerUris, discoveredServerUri, savedServerUri) {
        when {
            prioritizedServerUris.isNotEmpty() -> prioritizedServerUris.first()
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

    val safePopBackStack: () -> Unit = {
        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(180))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                targetOffset = { it / 4 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                initialOffset = { it / 4 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(180))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        }
    ) {
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
                tunnelUri = savedTunnelUri,
                token = activeAccessToken ?: "",
                viewModel = fileViewModel,
                prioritizedServerUris = prioritizedServerUris,
                discoveredUris = discoveredServerUris.toList(),
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("drive") { inclusive = true }
                        }
                    }
                },
                onNavigateToConnectionManager = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate("connection_manager") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Backward-compatibility aliases
        composable("me") {
            AaxionDriveScreen(
                serverUri = activeServerUri,
                tunnelUri = savedTunnelUri,
                token = activeAccessToken ?: "",
                viewModel = fileViewModel,
                prioritizedServerUris = prioritizedServerUris,
                discoveredUris = discoveredServerUris.toList(),
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("me") { inclusive = true }
                        }
                    }
                },
                onNavigateToConnectionManager = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate("connection_manager") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("explorer") {
            AaxionDriveScreen(
                serverUri = activeServerUri,
                tunnelUri = savedTunnelUri,
                token = activeAccessToken ?: "",
                viewModel = fileViewModel,
                prioritizedServerUris = prioritizedServerUris,
                discoveredUris = discoveredServerUris.toList(),
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("explorer") { inclusive = true }
                        }
                    }
                },
                onNavigateToConnectionManager = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate("connection_manager") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                serverUri = activeServerUri,
                token = activeAccessToken ?: "",
                onBackClick = safePopBackStack,
                onLogout = {
                    scope.launch {
                        tokenManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("drive") { inclusive = true }
                        }
                    }
                },
                onNavigateToConnectionManager = {
                    if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate("connection_manager") {
                            launchSingleTop = true
                        }
                    }
                },
                wrapWithBackground = true
            )
        }

        composable("connection_manager") {
            ConnectionManagerScreen(
                discoveredUris = discoveredServerUris.toList(),
                currentActiveUri = activeServerUri,
                initialPrioritizedUris = prioritizedServerUris,
                initialTunnelUri = savedTunnelUri,
                onBackClick = safePopBackStack,
                onRefresh = {
                    discoveredServerUris.clear()
                    refreshDiscovery.value += 1
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

    val accentColor = LocalAccentColor.current

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
                    .background(accentColor.copy(alpha = 0.12f * alpha))
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = 0.6f * alpha),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = alpha),
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

