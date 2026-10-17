package com.codershubinc.aaxion_mob.feature.drive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import com.codershubinc.aaxion_mob.R
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.codershubinc.aaxion_mob.core.data.DataStoreManager
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.network.ServerPingHelper
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.feature.settings.SettingsScreen
import com.codershubinc.aaxion_mob.feature.music.MusicView
import com.codershubinc.aaxion_mob.feature.home.HomeScreen
import com.codershubinc.aaxion_mob.feature.storage.StorageAndHubView
import com.codershubinc.aaxion_mob.feature.storage.StorageInfoTile
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AaxionDriveScreen(
    serverUri: String,
    tunnelUri: String? = null,
    token: String,
    viewModel: FileViewModel,
    prioritizedServerUris: List<String> = emptyList(),
    discoveredUris: List<String> = emptyList(),
    onLogout: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 768
    val isDesktop = screenWidth > 1024
    val isTablet = !isMobile && !isDesktop

    val tokenManager = remember { TokenManager.getInstance(context) }
    val isRefreshingToken by tokenManager.isRefreshing.collectAsState()
    val lastRefreshTs by tokenManager.lastRefreshTimestamp.collectAsState()
    val activeAccessToken by tokenManager.accessTokenState.collectAsState()
    val activeMasterToken by tokenManager.masterTokenState.collectAsState()

    val effectiveToken = if (!activeAccessToken.isNullOrBlank()) activeAccessToken!! else token

    // Tabs: 0 = Drive, 1 = Music, 2 = Storage & Tokens, 3 = Settings
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var isGridView by rememberSaveable { mutableStateOf(true) }
    var isSidebarVisible by remember(isDesktop) { mutableStateOf(isDesktop) }

    val playerManager = remember { MediaPlayerManager.getInstance(context) }
    val playerState by playerManager.state.collectAsState()

    // Live server ping and latency
    var serverLatencyMs by remember { mutableStateOf<Long?>(null) }
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isLocalConnection by remember { mutableStateOf(true) }
    var activeServerUri by remember(serverUri) { mutableStateOf(serverUri) }

    LaunchedEffect(serverUri) {
        if (serverUri.isNotBlank()) {
            activeServerUri = serverUri
        }
    }

    // Intercept system Back button: Close player if expanded, close sidebar drawer, clear active search, return to Drive tab, or navigate back in dir tree
    BackHandler(
        enabled = playerState.isExpanded ||
                (isSidebarVisible && !isDesktop) ||
                viewModel.searchQuery.isNotBlank() ||
                (currentTab == 1 && viewModel.canNavigateUp) ||
                currentTab != 0
    ) {
        if (playerState.isExpanded) {
            playerManager.setExpanded(false)
        } else if (isSidebarVisible && !isDesktop) {
            isSidebarVisible = false
        } else if (viewModel.searchQuery.isNotBlank()) {
            viewModel.searchQuery = ""
        } else if (currentTab == 1 && viewModel.canNavigateUp) {
            viewModel.navigateUp(activeServerUri, effectiveToken, tokenManager = tokenManager)
        } else if (currentTab != 0) {
            currentTab = 0
        }
    }

    LaunchedEffect(prioritizedServerUris, discoveredUris, tunnelUri) {
        while (isActive) {
            var found = false
            // 1. Try Prioritized URIs in order
            for (uri in prioritizedServerUris) {
                if (uri.isBlank()) continue
                val lat = ServerPingHelper.pingServer(uri = uri)
                if (lat != null) {
                    isServerOnline = true
                    isLocalConnection = true
                    serverLatencyMs = lat
                    activeServerUri = uri
                    found = true
                    break
                }
            }

            // 2. Try Discovered URIs
            if (!found) {
                val remainingUris = discoveredUris.filter { !prioritizedServerUris.contains(it) && it.isNotBlank() }
                for (uri in remainingUris) {
                    val lat = ServerPingHelper.pingServer(uri = uri)
                    if (lat != null) {
                        isServerOnline = true
                        isLocalConnection = true
                        serverLatencyMs = lat
                        activeServerUri = uri
                        found = true
                        break
                    }
                }
            }

            // 3. Try Tunnel URI
            if (!found && !tunnelUri.isNullOrBlank()) {
                val lat = ServerPingHelper.pingServer(uri = tunnelUri!!)
                if (lat != null) {
                    isServerOnline = true
                    isLocalConnection = false
                    serverLatencyMs = lat
                    activeServerUri = tunnelUri!!
                    found = true
                }
            }

            if (!found) {
                isServerOnline = false
                serverLatencyMs = null
            }

            delay(8000L)
        }
    }

    // Initial load
    LaunchedEffect(activeServerUri, effectiveToken) {
        if (activeServerUri.isNotBlank() && effectiveToken.isNotBlank()) {
            if (viewModel.files.isEmpty()) {
                viewModel.fetchFiles(activeServerUri, effectiveToken, "/", tokenManager = tokenManager)
            }
        }
    }

    AmoledGlassBackground(accentColor = accentColor) {
        if (!isMobile) {
            // ==========================================
            // TABLET/DESKTOP RESPONSIVE LAYOUT (Collapsible / Fixed Sidebar)
            // ==========================================
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content Canvas (Uses full screen or left-padded for desktop sidebar)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(start = if (isDesktop && isSidebarVisible) 268.dp else 0.dp)
                ) {
                    when (currentTab) {
                        0 -> HomeScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onNavigateToDrive = { path ->
                                currentTab = 1
                                if (path != null) {
                                    viewModel.navigateTo(activeServerUri, effectiveToken, path, tokenManager = tokenManager)
                                }
                            },
                            onNavigateToMusic = { currentTab = 2 },
                            onNavigateToStorage = { currentTab = 3 },
                            onNavigateToSettings = { currentTab = 4 },
                            onNavigateToConnectionManager = onNavigateToConnectionManager,
                            isServerOnline = isServerOnline,
                            serverLatencyMs = serverLatencyMs,
                            isTablet = true
                        )
                        1 -> DriveFilesView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isGridView = isGridView,
                            onToggleView = { isGridView = !isGridView },
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            onOpenSettings = {
                                currentTab = 4
                            },
                            isTablet = true,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection,
                            onOpenConnectionManager = onNavigateToConnectionManager
                        )
                        2 -> MusicView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            tokenManager = tokenManager,
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            isTablet = true
                        )
                        3 -> StorageAndHubView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            masterToken = activeMasterToken,
                            lastRefreshTs = lastRefreshTs,
                            isRefreshing = isRefreshingToken,
                            onSelectStorageDevice = { targetPath ->
                                currentTab = 1
                                if (!isDesktop) {
                                    isSidebarVisible = false
                                }
                                viewModel.navigateTo(activeServerUri, effectiveToken, targetPath, tokenManager = tokenManager)
                            },
                            onRefreshToken = {
                                scope.launch {
                                    val success = tokenManager.refreshAccessToken(activeServerUri)
                                    val msg = if (success) "Access token refreshed" else "Refresh failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenIpSelector = onNavigateToConnectionManager,
                            onLogout = onLogout
                        )
                        4 -> SettingsScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onBackClick = { currentTab = 0 },
                            onLogout = onLogout,
                            onNavigateToConnectionManager = onNavigateToConnectionManager
                        )
                    }
                }

            }
        } else {
            // ==========================================
            // MOBILE RESPONSIVE LAYOUT (Full Screen + Bottom Bar)
            // ==========================================
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    val dockShape = RoundedCornerShape(28.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .clip(dockShape)
                            .background(GlassBrushes.glassSurface())
                            .border(
                                width = 0.8.dp,
                                brush = GlassBrushes.specularBorder(accentColor),
                                shape = dockShape
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(62.dp)
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                Triple("Home", Icons.Default.Home, 0),
                                Triple("Drive", Icons.Default.CloudQueue, 1),
                                Triple("Music", Icons.Default.Headphones, 2),
                                Triple("Storage", Icons.Default.Storage, 3),
                                Triple("Settings", Icons.Default.Settings, 4)
                            )

                            tabs.forEach { (title, icon, index) ->
                                val isSelected = currentTab == index
                                val tabColor = if (index == 2) StreamerGreen else accentColor

                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.05f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "dock_tab_scale"
                                )

                                val pillAlpha by animateFloatAsState(
                                    targetValue = if (isSelected) 0.16f else 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "dock_pill_alpha"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(tabColor.copy(alpha = pillAlpha))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = tabColor),
                                            onClick = {
                                                currentTab = index
                                            }
                                        )
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = if (isSelected) tabColor else TextTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = title,
                                            color = if (isSelected) tabColor else TextTertiary,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        0 -> HomeScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onNavigateToDrive = { path ->
                                currentTab = 1
                                if (path != null) {
                                    viewModel.navigateTo(activeServerUri, effectiveToken, path, tokenManager = tokenManager)
                                }
                            },
                            onNavigateToMusic = { currentTab = 2 },
                            onNavigateToStorage = { currentTab = 3 },
                            onNavigateToSettings = { currentTab = 4 },
                            onNavigateToConnectionManager = onNavigateToConnectionManager,
                            isServerOnline = isServerOnline,
                            serverLatencyMs = serverLatencyMs,
                            isTablet = false
                        )
                        1 -> DriveFilesView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isGridView = isGridView,
                            onToggleView = { isGridView = !isGridView },
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            onOpenSettings = {
                                currentTab = 4
                            },
                            isTablet = false,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection,
                            onOpenConnectionManager = onNavigateToConnectionManager
                        )
                        2 -> MusicView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            tokenManager = tokenManager,
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            isTablet = false
                        )
                        3 -> StorageAndHubView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            masterToken = activeMasterToken,
                            lastRefreshTs = lastRefreshTs,
                            isRefreshing = isRefreshingToken,
                            onSelectStorageDevice = { targetPath ->
                                currentTab = 1
                                if (!isDesktop) {
                                    isSidebarVisible = false
                                }
                                viewModel.navigateTo(activeServerUri, effectiveToken, targetPath, tokenManager = tokenManager)
                            },
                            onRefreshToken = {
                                scope.launch {
                                    val success = tokenManager.refreshAccessToken(activeServerUri)
                                    val msg = if (success) "Access token refreshed" else "Refresh failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenIpSelector = onNavigateToConnectionManager,
                            onLogout = onLogout
                        )
                        4 -> SettingsScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onBackClick = { currentTab = 0 },
                            onLogout = onLogout,
                            onNavigateToConnectionManager = onNavigateToConnectionManager
                        )
                    }
                }
            }
        }
        Sidebar(
            isVisible = isSidebarVisible,
            isDesktop = isDesktop,
            serverUri = activeServerUri,
            activeServerUri = activeServerUri,
            effectiveToken = effectiveToken,
            prioritizedServerUris = prioritizedServerUris,
            currentTab = currentTab,
            onTabSelected = { tab ->
                currentTab = tab
                if (!isDesktop) {
                    isSidebarVisible = false
                }
            },
            onCloseSidebar = { isSidebarVisible = false },
            onNavigateToConnectionManager = {
                if (!isDesktop) {
                    isSidebarVisible = false
                }
                onNavigateToConnectionManager()
            },
            onSelectStorageDevice = { targetPath ->
                currentTab = 1
                if (!isDesktop) {
                    isSidebarVisible = false
                }
                viewModel.navigateTo(activeServerUri, effectiveToken, targetPath, tokenManager = tokenManager)
            }
        )

        // Built-in Floating Island Media Player (background streaming + capsule / card viewer docked at bottom)
        FloatingIslandPlayer(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

