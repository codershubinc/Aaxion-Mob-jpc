package com.codershubinc.aaxion_mob.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.codershubinc.aaxion_mob.auth.TokenManager
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.models.StorageInfoResponse
import com.codershubinc.aaxion_mob.ui.components.AaxionCard
import com.codershubinc.aaxion_mob.ui.components.InteractiveBreadcrumbs
import com.codershubinc.aaxion_mob.ui.components.IpSelectorModal
import com.codershubinc.aaxion_mob.ui.components.StorageInfoTile
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.ui.views.DriveFilesView
import com.codershubinc.aaxion_mob.ui.views.SidebarStorageDevices
import com.codershubinc.aaxion_mob.ui.views.StorageAndHubView
import com.codershubinc.aaxion_mob.ui.views.StreamerMediaView
import com.codershubinc.aaxion_mob.utils.DataStoreManager
import com.codershubinc.aaxion_mob.utils.Fetcher
import com.codershubinc.aaxion_mob.utils.IntentUtils
import com.codershubinc.aaxion_mob.viewmodels.FileCategory
import com.codershubinc.aaxion_mob.viewmodels.FileViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AaxionDriveScreen(
    serverUri: String,
    tunnelUri: String? = null,
    token: String,
    viewModel: FileViewModel,
    discoveredUris: List<String> = emptyList(),
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isMobile = screenWidth < 768
    val isDesktop = screenWidth > 1024
    val isTablet = !isMobile && !isDesktop

    val dataStoreManager = remember { DataStoreManager(context) }
    val prioritizedServerUri by dataStoreManager.prioritizedServerUri.collectAsState(initial = null)
    var showIpSelectorModal by remember { mutableStateOf(false) }

    val tokenManager = remember { TokenManager.getInstance(context) }
    val isRefreshingToken by tokenManager.isRefreshing.collectAsState()
    val lastRefreshTs by tokenManager.lastRefreshTimestamp.collectAsState()
    val activeAccessToken by tokenManager.accessTokenState.collectAsState()
    val activeMasterToken by tokenManager.masterTokenState.collectAsState()

    val effectiveToken = if (!activeAccessToken.isNullOrBlank()) activeAccessToken!! else token

    // Tabs: 0 = Drive, 1 = Streamer, 2 = Storage & Tokens, 3 = Settings
    var currentTab by remember { mutableIntStateOf(0) }
    var isGridView by remember { mutableStateOf(true) }
    var isSidebarVisible by remember(isDesktop) { mutableStateOf(isDesktop) }

    // Intercept system Back button: Navigate back in dir tree first, or return to Drive tab
    BackHandler(enabled = (viewModel.currentPath != "/" && viewModel.currentPath.isNotEmpty()) || currentTab != 0) {
        if (viewModel.currentPath != "/" && viewModel.currentPath.isNotEmpty()) {
            viewModel.navigateUp(serverUri, effectiveToken, tokenManager = tokenManager)
        } else if (currentTab != 0) {
            currentTab = 0
        }
    }

    // Modal for selecting and prioritizing server IP
    if (showIpSelectorModal) {
        IpSelectorModal(
            currentActiveUri = serverUri,
            prioritizedUri = prioritizedServerUri,
            discoveredUris = discoveredUris,
            onSelectAndPrioritize = { selectedUri ->
                scope.launch {
                    dataStoreManager.setPrioritizedServerUri(selectedUri)
                    showIpSelectorModal = false
                }
            },
            onClearPriority = {
                scope.launch {
                    dataStoreManager.setPrioritizedServerUri(null)
                    showIpSelectorModal = false
                }
            },
            onDismiss = { showIpSelectorModal = false }
        )
    }

    // Live server ping and latency
    var serverLatencyMs by remember { mutableStateOf<Long?>(null) }
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isLocalConnection by remember { mutableStateOf(true) }
    var activeServerUri by remember(serverUri) { mutableStateOf(serverUri) }

    LaunchedEffect(serverUri, tunnelUri) {
        while (isActive) {
            val client = Fetcher.getInstance().getClient()
            
            suspend fun ping(uri: String): Long? {
                if (uri.isBlank()) return null
                val cleanUri = if (uri.endsWith("/")) uri.dropLast(1) else uri
                val pingUrl = "$cleanUri/api/v1/system/storage"
                return try {
                    val request = okhttp3.Request.Builder().url(pingUrl).head().build()
                    val startTime = System.currentTimeMillis()
                    val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        try { client.newCall(request).execute() } catch (e: Exception) { null }
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    if (response != null && (response.isSuccessful || response.code in 200..499)) {
                        response.close()
                        elapsed
                    } else null
                } catch (e: Exception) { null }
            }

            if (serverUri.isNotBlank()) {
                val localLat = ping(serverUri)
                if (localLat != null) {
                    isServerOnline = true
                    isLocalConnection = true
                    serverLatencyMs = localLat
                    activeServerUri = serverUri
                } else if (!tunnelUri.isNullOrBlank()) {
                    val tunnelLat = ping(tunnelUri!!)
                    if (tunnelLat != null) {
                        isServerOnline = true
                        isLocalConnection = false
                        serverLatencyMs = tunnelLat
                        activeServerUri = tunnelUri!!
                    } else {
                        isServerOnline = false
                        serverLatencyMs = null
                    }
                } else {
                    isServerOnline = false
                    serverLatencyMs = null
                }
            } else {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AaxionBg)
    ) {
        if (!isMobile) {
            // ==========================================
            // TABLET/DESKTOP RESPONSIVE LAYOUT (Collapsible / Fixed Sidebar)
            // ==========================================
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content Canvas (Uses full screen or left-padded for desktop sidebar)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = if (isDesktop && isSidebarVisible) 268.dp else 0.dp)
                ) {
                    when (currentTab) {
                        0 -> DriveFilesView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isGridView = isGridView,
                            onToggleView = { isGridView = !isGridView },
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            onOpenSettings = { currentTab = 3 },
                            isTablet = true,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection
                        )
                        1 -> StreamerMediaView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            isTablet = true
                        )
                        2 -> StorageAndHubView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            masterToken = activeMasterToken,
                            lastRefreshTs = lastRefreshTs,
                            isRefreshing = isRefreshingToken,
                            onRefreshToken = {
                                scope.launch {
                                    val success = tokenManager.refreshAccessToken(serverUri)
                                    val msg = if (success) "Access token refreshed" else "Refresh failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenIpSelector = { showIpSelectorModal = true },
                            onLogout = onLogout
                        )
                        3 -> SettingsScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onBackClick = { currentTab = 0 },
                            onLogout = onLogout
                        )
                    }
                }

            }
        } else {
            isServerOnline = false
            serverLatencyMs = null
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AaxionBg)
    ) {
        if (!isMobile) {
            // ==========================================
            // TABLET/DESKTOP RESPONSIVE LAYOUT (Collapsible / Fixed Sidebar)
            // ==========================================
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content Canvas (Uses full screen or left-padded for desktop sidebar)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = if (isDesktop && isSidebarVisible) 268.dp else 0.dp)
                ) {
                    when (currentTab) {
                        0 -> DriveFilesView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isGridView = isGridView,
                            onToggleView = { isGridView = !isGridView },
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            onOpenSettings = { currentTab = 3 },
                            isTablet = true,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection
                        )
                        1 -> StreamerMediaView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            isTablet = true
                        )
                        2 -> StorageAndHubView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            masterToken = activeMasterToken,
                            lastRefreshTs = lastRefreshTs,
                            isRefreshing = isRefreshingToken,
                            onRefreshToken = {
                                scope.launch {
                                    val success = tokenManager.refreshAccessToken(serverUri)
                                    val msg = if (success) "Access token refreshed" else "Refresh failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenIpSelector = { showIpSelectorModal = true },
                            onLogout = onLogout
                        )
                        3 -> SettingsScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onBackClick = { currentTab = 0 },
                            onLogout = onLogout
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
                bottomBar = {
                    NavigationBar(
                        containerColor = AaxionSurface,
                        contentColor = TextPrimary,
                        tonalElevation = 0.dp,
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = AaxionBorder,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Drive") },
                            label = { Text("Drive", fontSize = 10.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AaxionPrimary,
                                selectedTextColor = AaxionPrimary,
                                indicatorColor = AaxionPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            icon = { Icon(Icons.Default.PlayCircleFilled, contentDescription = "Streamer") },
                            label = { Text("Streamer", fontSize = 10.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StreamerOrange,
                                selectedTextColor = StreamerOrange,
                                indicatorColor = StreamerOrange.copy(alpha = 0.15f),
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { currentTab = 2 },
                            icon = { Icon(Icons.Default.Storage, contentDescription = "Storage") },
                            label = { Text("Storage", fontSize = 10.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AaxionElectricBlue,
                                selectedTextColor = AaxionElectricBlue,
                                indicatorColor = AaxionElectricBlue.copy(alpha = 0.15f),
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { currentTab = 3 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 10.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AaxionPrimary,
                                selectedTextColor = AaxionPrimary,
                                indicatorColor = AaxionPrimary.copy(alpha = 0.15f),
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            )
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        0 -> DriveFilesView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isGridView = isGridView,
                            onToggleView = { isGridView = !isGridView },
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            onOpenSettings = { currentTab = 3 },
                            isTablet = false,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection
                        )
                        1 -> StreamerMediaView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            viewModel = viewModel,
                            isSidebarVisible = isSidebarVisible,
                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },
                            isTablet = false
                        )
                        2 -> StorageAndHubView(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            masterToken = activeMasterToken,
                            lastRefreshTs = lastRefreshTs,
                            isRefreshing = isRefreshingToken,
                            showStorageDevices = true,
                            onSelectStorageDevice = { targetPath ->
                                currentTab = 0
                                viewModel.navigateTo(serverUri, effectiveToken, targetPath, tokenManager = tokenManager)
                            },
                            onRefreshToken = {
                                scope.launch {
                                    val success = tokenManager.refreshAccessToken(serverUri)
                                    val msg = if (success) "Access token refreshed" else "Refresh failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenIpSelector = { showIpSelectorModal = true },
                            onLogout = onLogout
                        )
                        3 -> SettingsScreen(
                            serverUri = activeServerUri,
                            token = effectiveToken,
                            onBackClick = { currentTab = 0 },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
        // Dimmed overlay for mobile/tablet to tap and close the sidebar
        if (!isDesktop) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSidebarVisible,
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(150)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { isSidebarVisible = false }
                )
            }
        }
                // Collapsible Sidebar with rounded corners (Overlaid with smooth slide, 0 frame drops)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSidebarVisible,
                    enter = androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(durationMillis = 200)
                    ) + androidx.compose.animation.fadeIn(animationSpec = tween(150)),
                    exit = androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = 200)
                    ) + androidx.compose.animation.fadeOut(animationSpec = tween(150)),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AaxionSurface)
                            .border(1.dp, AaxionBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Section: Header & Nav
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header with Hide Sidebar button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.aaxion_logo),
                                        contentDescription = "Aaxion logo",
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showIpSelectorModal = true }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AAXION",
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val displayHost = remember(serverUri) {
                                                try {
                                                    val u = android.net.Uri.parse(serverUri)
                                                    u.host ?: serverUri
                                                } catch (e: Exception) {
                                                    serverUri
                                                }
                                            }
                                            Text(
                                                text = displayHost.ifBlank { "No Server" },
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!prioritizedServerUri.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Pinned IP",
                                                    tint = AaxionPrimary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isDesktop) {
                                    IconButton(
                                        onClick = { isSidebarVisible = false },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                                            contentDescription = "Hide Sidebar",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Navigation Items
                            TabletSidebarItem(
                                icon = Icons.Default.Folder,
                                label = "My Drive",
                                isSelected = currentTab == 0,
                                onClick = { currentTab = 0 }
                            )
                        }

                        // Bottom Section: Storage Devices Tiles & Settings Icon
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            HorizontalDivider(color = AaxionBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Storage Device Tiles directly at bottom of sidebar!
                            SidebarStorageDevices(
                                serverUri = activeServerUri,
                                token = effectiveToken,
                                onSelectStorageDevice = { targetPath ->
                                    currentTab = 0
                                    viewModel.navigateTo(serverUri, effectiveToken, targetPath, tokenManager = tokenManager)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = AaxionBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Settings Action Icon at Bottom
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { currentTab = 3 }
                                    .background(if (currentTab == 3) AaxionPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (currentTab == 3) AaxionPrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Settings",
                                    color = if (currentTab == 3) AaxionPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

    }
}

// Tablet Sidebar Item
@Composable
private fun TabletSidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AaxionPrimary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) AaxionPrimary else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
    }
}

