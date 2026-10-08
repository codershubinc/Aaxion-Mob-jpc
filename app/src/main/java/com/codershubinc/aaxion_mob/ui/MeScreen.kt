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

@Composable
private fun DriveTabContent(
    serverUri: String,
    token: String,
    onExploreFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Storage Info Card
        StorageInfoTile(
            serverUri = serverUri,
            token = token
        )

        // Quick File Explorer Action Card
        AaxionCard(
            backgroundColor = AaxionSurfaceVariant,
            borderColor = AaxionBorder,
            onClick = onExploreFiles
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AaxionPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = AaxionPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Browse File System",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open directory tree, stream, and download files",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Quick Media Categories Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCategoryCard(
                title = "Videos",
                subtitle = "Streamable",
                color = StreamerOrange,
                icon = Icons.Default.Movie,
                modifier = Modifier.weight(1f),
                onClick = onExploreFiles
            )
            QuickCategoryCard(
                title = "Music",
                subtitle = "Audio tracks",
                color = StreamerGreen,
                icon = Icons.Default.MusicNote,
                modifier = Modifier.weight(1f),
                onClick = onExploreFiles
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCategoryCard(
                title = "Photos",
                subtitle = "Thumbnails",
                color = StreamerPink,
                icon = Icons.Default.Image,
                modifier = Modifier.weight(1f),
                onClick = onExploreFiles
            )
            QuickCategoryCard(
                title = "Documents",
                subtitle = "Archives & files",
                color = StreamerPurple,
                icon = Icons.Default.Description,
                modifier = Modifier.weight(1f),
                onClick = onExploreFiles
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StreamerTabContent(
    serverUri: String,
    token: String,
    mediaFiles: List<FileItem>,
    isLoading: Boolean,
    onExploreFiles: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Streamer Banner
        AaxionCard(
            backgroundColor = AaxionSurfaceVariant,
            borderColor = AaxionBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.OndemandVideo,
                    contentDescription = null,
                    tint = StreamerOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Aaxion Streamer Hub",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stream videos & audio directly to your device player",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STREAMABLE MEDIA IN ROOT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextTertiary
            )
            Text(
                text = "Browse All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AaxionPrimary,
                modifier = Modifier.clickable { onExploreFiles() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StreamerOrange, strokeWidth = 2.dp)
            }
        } else if (mediaFiles.isEmpty()) {
            AaxionCard(
                backgroundColor = AaxionSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No direct media files in root",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Browse directories to locate and stream your videos and music",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onExploreFiles,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AaxionPrimaryDark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open File Explorer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mediaFiles) { file ->
                    val isVideo = FileViewModel.isVideoFile(file.name)
                    val encoded = try {
                        URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                    } catch (e: Exception) {
                        file.rawPath
                    }
                    val streamUrl = "$serverUri/api/v1/files/stream?path=$encoded&tkn=$token"

                    AaxionCard(
                        backgroundColor = AaxionSurfaceVariant,
                        borderColor = AaxionBorder,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(streamUrl), if (isVideo) "video/*" else "audio/*")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl)))
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isVideo) StreamerOrange.copy(alpha = 0.15f) else StreamerGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Default.PlayCircleOutline else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isVideo) StreamerOrange else StreamerGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isVideo) "Tap to stream video" else "Tap to stream audio track",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isVideo) StreamerOrange else StreamerGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubTabContent(
    serverUri: String,
    masterToken: String?,
    accessToken: String,
    lastRefreshTs: Long,
    isRefreshing: Boolean,
    onRefreshToken: () -> Unit,
    onLogout: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Server Info Tile
        AaxionCard(
            backgroundColor = AaxionSurfaceVariant,
            borderColor = AaxionBorder
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = AaxionPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connected Server",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = serverUri.ifBlank { "Auto-discovered Host" },
                color = AaxionPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Token Architecture & 4-Hour Refresh Card (online badge removed)
        AaxionCard(
            backgroundColor = AaxionSurfaceVariant,
            borderColor = AaxionBorder
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = AaxionPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Session Security Tokens",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Master Token Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AaxionSurface)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StreamerOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Master Token",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (!masterToken.isNullOrBlank()) "••••••••••••${masterToken.takeLast(6)}" else "Not available",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Used exclusively to regenerate access tokens every 4 hours.",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Access Token Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AaxionSurface)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = AaxionPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Access Token (Bearer)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (accessToken.isNotBlank()) "••••••••••••${accessToken.takeLast(6)}" else "Not available",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Attached to all Drive & Streamer API calls.",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Refresh Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LAST REFRESHED",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (lastRefreshTs > 0) dateFormat.format(Date(lastRefreshTs)) else "Upon sign-in",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onRefreshToken,
                    enabled = !isRefreshing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AaxionPrimaryDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Logout Session Card
        AaxionCard(
            backgroundColor = AaxionDanger.copy(alpha = 0.06f),
            borderColor = AaxionDanger.copy(alpha = 0.2f),
            onClick = onLogout
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AaxionDanger.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = AaxionDanger,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sign Out & Terminate Session",
                        color = AaxionDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Clears master token, access token, and credentials",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun QuickCategoryCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AaxionCard(
        modifier = modifier,
        backgroundColor = AaxionSurfaceVariant,
        borderColor = AaxionBorder,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = TextTertiary,
            fontSize = 11.sp
        )
    }
}
