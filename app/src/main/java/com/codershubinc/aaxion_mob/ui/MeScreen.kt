package com.codershubinc.aaxion_mob.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.auth.TokenManager
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.ui.components.AaxionCard
import com.codershubinc.aaxion_mob.ui.components.StorageInfoTile
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.ui.views.me.*
import com.codershubinc.aaxion_mob.utils.Fetcher
import com.codershubinc.aaxion_mob.viewmodels.FileViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeScreen(
    serverUri: String,
    token: String,
    onExploreFiles: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val tokenManager = remember { TokenManager.getInstance(context) }
    val isRefreshingToken by tokenManager.isRefreshing.collectAsState()
    val lastRefreshTs by tokenManager.lastRefreshTimestamp.collectAsState()
    val activeAccessToken by tokenManager.accessTokenState.collectAsState()
    val activeMasterToken by tokenManager.masterTokenState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Drive, 1: Streamer, 2: Session & Hub

    // Media scan for streamer tab
    var mediaFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoadingMedia by remember { mutableStateOf(false) }

    val effectiveToken = if (!activeAccessToken.isNullOrBlank()) activeAccessToken!! else token

    LaunchedEffect(serverUri, effectiveToken, selectedTab) {
        if (selectedTab == 1 && serverUri.isNotBlank() && effectiveToken.isNotBlank()) {
            isLoadingMedia = true
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = service.listFiles("/", "Bearer $effectiveToken")
                if (response.isSuccessful && response.body() != null) {
                    mediaFiles = response.body()!!.filter {
                        !it.isDir && (FileViewModel.isVideoFile(it.name) || FileViewModel.isAudioFile(it.name))
                    }
                }
            } catch (e: Exception) {
                // Silently handle media load error
            } finally {
                isLoadingMedia = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AaxionBg)
    ) {
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
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Drive"
                            )
                        },
                        label = {
                            Text(
                                "Drive",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AaxionPrimary,
                            selectedTextColor = AaxionPrimary,
                            indicatorColor = AaxionPrimary.copy(alpha = 0.15f),
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Streamer"
                            )
                        },
                        label = {
                            Text(
                                "Streamer",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = StreamerOrange,
                            selectedTextColor = StreamerOrange,
                            indicatorColor = StreamerOrange.copy(alpha = 0.15f),
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Session"
                            )
                        },
                        label = {
                            Text(
                                "Hub & Tokens",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AaxionElectricBlue,
                            selectedTextColor = AaxionElectricBlue,
                            indicatorColor = AaxionElectricBlue.copy(alpha = 0.15f),
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
                    .padding(horizontal = if (isTablet) 36.dp else 20.dp)
            ) {
                // Top Header (online indicator removed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AAXION",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = when (selectedTab) {
                                0 -> "Cloud Storage & Explorer"
                                1 -> "High-Definition Media Streamer"
                                else -> "Auth & Security Control Center"
                            },
                            color = when (selectedTab) {
                                0 -> AaxionPrimary
                                1 -> StreamerOrange
                                else -> AaxionElectricBlue
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> DriveTabContent(
                        serverUri = serverUri,
                        token = effectiveToken,
                        onExploreFiles = onExploreFiles
                    )
                    1 -> StreamerTabContent(
                        serverUri = serverUri,
                        token = effectiveToken,
                        mediaFiles = mediaFiles,
                        isLoading = isLoadingMedia,
                        onExploreFiles = onExploreFiles
                    )
                    2 -> HubTabContent(
                        serverUri = serverUri,
                        masterToken = activeMasterToken,
                        accessToken = effectiveToken,
                        lastRefreshTs = lastRefreshTs,
                        isRefreshing = isRefreshingToken,
                        onRefreshToken = {
                            scope.launch {
                                val success = tokenManager.refreshAccessToken(serverUri)
                                val msg = if (success) "Access token refreshed (valid for 4 hours)" else "Failed to refresh token"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

