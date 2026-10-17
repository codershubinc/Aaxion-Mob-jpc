package com.codershubinc.aaxion_mob.feature.home

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.RecentMediaItem
import com.codershubinc.aaxion_mob.core.media.RecentPlaybackManager
import com.codershubinc.aaxion_mob.core.media.VideoProgress
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.feature.drive.viwerModals.VideoResumeModal
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * Home dashboard featuring server health, interactive storage tiles,
 * recent media playback tiles with resume capabilities, and quick navigation shortcuts.
 */
@Composable
fun HomeScreen(
    serverUri: String,
    token: String,
    onNavigateToDrive: (path: String?) -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    isServerOnline: Boolean? = true,
    serverLatencyMs: Long? = null,
    isTablet: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    val recentManager = remember { RecentPlaybackManager.getInstance(context) }
    val vlcManager = remember { VlcPlayerManager.getInstance(context) }

    val recentItems by recentManager.recentItems.collectAsState()

    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isStorageLoading by remember { mutableStateOf(true) }
    var storageRefreshKey by remember { mutableIntStateOf(0) }

    var pendingResumeVideo by remember { mutableStateOf<Triple<FileItem, String, VideoProgress>?>(null) }

    // Fetch storage volumes
    LaunchedEffect(serverUri, token, storageRefreshKey) {
        if (serverUri.isNotBlank() && token.isNotBlank()) {
            isStorageLoading = true
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = tokenManager.withAuthRetry(serverUri, token) { auth ->
                    service.getStorageInfo("Bearer $auth")
                }
                if (response.isSuccessful && response.body() != null) {
                    storageInfo = response.body()
                }
            } catch (e: Exception) {
                // ignore network error
            } finally {
                isStorageLoading = false
            }
        } else {
            isStorageLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Server Banner
        HomeHeaderBanner(
            serverUri = serverUri,
            isOnline = isServerOnline ?: true,
            latencyMs = serverLatencyMs,
            onOpenConnectionManager = onNavigateToConnectionManager,
            accentColor = accentColor
        )

        // Storage Volumes Section
        HomeStorageSection(
            storageInfo = storageInfo,
            isLoading = isStorageLoading,
            onRefresh = { storageRefreshKey++ },
            onSelectVolume = { path -> onNavigateToDrive(path) },
            accentColor = accentColor
        )

        // Recent Played Media Section
        HomeRecentPlayedSection(
            recentItems = recentItems,
            serverUri = serverUri,
            token = token,
            onPlayAudio = { item ->
                val streamUrl = item.toFileItem().buildStreamUrl(serverUri, token)
                MediaPlayerManager.getInstance(context).play(
                    file = item.toFileItem(),
                    streamUrl = streamUrl,
                    token = token,
                    baseUrl = serverUri,
                    displayTitle = item.title,
                    displayArtist = item.subtitle
                )
            },
            onPlayVideo = { item ->
                val streamUrl = item.toFileItem().buildStreamUrl(serverUri, token)
                val file = item.toFileItem()
                val progress = vlcManager.getProgress(item.rawPath)
                if (progress != null && progress.positionMs >= 5000L) {
                    pendingResumeVideo = Triple(file, streamUrl, progress)
                } else {
                    vlcManager.launchVlc(context, file, streamUrl, resume = false)
                }
            },
            onClearHistory = {
                recentManager.clearAll()
                Toast.makeText(context, "Playback history cleared", Toast.LENGTH_SHORT).show()
            },
            onRemoveItem = { rawPath ->
                recentManager.removeItem(rawPath)
            },
            accentColor = accentColor
        )

        // Quick Shortcuts Section
        HomeShortcutsSection(
            onNavigateToDrive = { onNavigateToDrive(null) },
            onNavigateToMusic = onNavigateToMusic,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToSettings = onNavigateToSettings,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Video Resume Confirmation Modal
    pendingResumeVideo?.let { (file, streamUrl, progress) ->
        VideoResumeModal(
            file = file,
            progress = progress,
            onResume = {
                vlcManager.launchVlc(context, file, streamUrl, resume = true)
                pendingResumeVideo = null
            },
            onStartOver = {
                vlcManager.launchVlc(context, file, streamUrl, resume = false)
                pendingResumeVideo = null
            },
            onPlayInternal = {
                MediaPlayerManager.getInstance(context).play(
                    file = file,
                    streamUrl = streamUrl,
                    token = token,
                    baseUrl = serverUri
                )
                pendingResumeVideo = null
            },
            onDismiss = { pendingResumeVideo = null }
        )
    }
}

/**
 * Header banner displaying connection status, host address, and quick network manager action.
 */
@Composable
private fun HomeHeaderBanner(
    serverUri: String,
    isOnline: Boolean,
    latencyMs: Long?,
    onOpenConnectionManager: () -> Unit,
    accentColor: Color
) {
    val host = remember(serverUri) {
        try {
            android.net.Uri.parse(serverUri).host ?: serverUri
        } catch (e: Exception) {
            serverUri
        }
    }

    InsetGroupedGlassCard(accentHighlight = accentColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenConnectionManager)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Aaxion Server",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) StreamerGreen else AaxionDanger)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (serverUri.isNotBlank()) host else "No server connected",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (latencyMs != null && isOnline) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, GlassHairline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${latencyMs}ms",
                        color = if (latencyMs < 50) StreamerGreen else if (latencyMs < 120) AaxionWarning else AaxionDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Section rendering the primary and external storage device tiles.
 */
@Composable
private fun HomeStorageSection(
    storageInfo: StorageInfoResponse?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSelectVolume: (path: String) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STORAGE VOLUMES",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(28.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 1.8.dp,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Storage",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (storageInfo != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary Root Storage Volume
                HomeStorageTile(
                    title = "Root System",
                    subtitleBadge = "SYSTEM",
                    mountPoint = "/",
                    icon = Icons.Rounded.Dns,
                    baseTint = accentColor,
                    usedBytes = storageInfo.used,
                    totalBytes = storageInfo.total,
                    usagePercentage = storageInfo.usagePercentage,
                    onClick = { onSelectVolume("/") }
                )

                // External Devices / USB Drives
                val externals = storageInfo.externalDevices ?: emptyList()
                externals.forEach { dev ->
                    val path = dev.mountPoint.ifBlank { "/" }
                    HomeStorageTile(
                        title = dev.device.substringAfterLast("/").ifBlank { "External Storage" },
                        subtitleBadge = "USB",
                        mountPoint = dev.mountPoint,
                        icon = Icons.Rounded.Usb,
                        baseTint = StreamerOrange,
                        usedBytes = dev.used,
                        totalBytes = dev.total,
                        usagePercentage = dev.usagePercentage,
                        onClick = { onSelectVolume(path) }
                    )
                }
            }
        } else if (isLoading) {
            InsetGroupedGlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                }
            }
        } else {
            InsetGroupedGlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Storage metrics unavailable",
                        color = TextTertiary,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Individual volume tile with progress meter and capacity breakdown.
 */
@Composable
private fun HomeStorageTile(
    title: String,
    subtitleBadge: String,
    mountPoint: String,
    icon: ImageVector,
    baseTint: Color,
    usedBytes: Long,
    totalBytes: Long,
    usagePercentage: Double,
    onClick: () -> Unit
) {
    val safeUsage = if (usagePercentage.isNaN()) 0.0 else usagePercentage
    val rawProgress = (safeUsage / 100f).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "home_storage_progress"
    )

    val statusTint = when {
        rawProgress >= 0.90f -> AaxionDanger
        rawProgress >= 0.75f -> AaxionWarning
        else -> baseTint
    }

    val freeBytes = (totalBytes - usedBytes).coerceAtLeast(0L)
    val tileShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tileShape)
            .background(GlassBrushes.glassSurface(accent = statusTint, isHighlighted = false))
            .border(0.8.dp, GlassBrushes.specularBorder(statusTint.copy(alpha = 0.35f)), tileShape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Icon + Title + Badge + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusTint.copy(alpha = 0.16f))
                        .border(0.5.dp, statusTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = statusTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusTint.copy(alpha = 0.18f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = subtitleBadge,
                                color = statusTint,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mountPoint,
                        color = TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${safeUsage.toInt()}%",
                    color = statusTint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusTint)
                )
            }

            // Footer Row: Used / Free Space
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${IntentUtils.formatFileSize(usedBytes)} used",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${IntentUtils.formatFileSize(freeBytes)} free",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Section rendering the recently played media tiles.
 */
@Composable
private fun HomeRecentPlayedSection(
    recentItems: List<RecentMediaItem>,
    serverUri: String,
    token: String,
    onPlayAudio: (RecentMediaItem) -> Unit,
    onPlayVideo: (RecentMediaItem) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveItem: (String) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RECENTLY PLAYED",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            if (recentItems.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onClearHistory)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        if (recentItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentItems.take(6).forEach { item ->
                    RecentMediaCard(
                        item = item,
                        serverUri = serverUri,
                        token = token,
                        onClick = {
                            if (item.isVideo) {
                                onPlayVideo(item)
                            } else {
                                onPlayAudio(item)
                            }
                        },
                        onRemove = { onRemoveItem(item.rawPath) },
                        accentColor = accentColor
                    )
                }
            }
        } else {
            InsetGroupedGlassCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Recent Playback",
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Music and videos you play will appear here for fast one-tap resume.",
                        color = TextTertiary,
                        fontSize = 11.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Individual card displaying a recently played media item.
 */
@Composable
private fun RecentMediaCard(
    item: RecentMediaItem,
    serverUri: String,
    token: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    accentColor: Color
) {
    val cardShape = RoundedCornerShape(14.dp)
    val mediaTint = if (item.isVideo) StreamerOrange else StreamerGreen

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(GlassBrushes.glassSurface())
            .border(0.6.dp, GlassHairline, cardShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork / Media Thumbnail
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(mediaTint.copy(alpha = 0.16f))
                    .border(0.5.dp, mediaTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = mediaTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle / Progress
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(mediaTint.copy(alpha = 0.14f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (item.isVideo) "VIDEO" else "AUDIO",
                            color = mediaTint,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val subtitleText = when {
                    item.isVideo && item.progressMs >= 5000L -> "Resume from ${item.formattedProgress}"
                    item.subtitle.isNotBlank() -> item.subtitle
                    item.durationMs > 0L -> "Duration: ${item.formattedDuration}"
                    else -> item.rawPath.substringAfterLast('/')
                }

                Text(
                    text = subtitleText,
                    color = if (item.isVideo && item.progressMs >= 5000L) StreamerOrange else TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Play Icon Button
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(mediaTint.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = mediaTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Shortcuts section linking to Drive, Music Library, Storage Hub, and Settings.
 */
@Composable
private fun HomeShortcutsSection(
    onNavigateToDrive: () -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "EXPLORE",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        InsetGroupedGlassCard {
            HomeShortcutRow(
                icon = Icons.Default.Folder,
                title = "Files & Storage Drive",
                subtitle = "Browse NAS directory filesystem",
                accentColor = accentColor,
                onClick = onNavigateToDrive
            )

            InsetGlassDivider(startIndent = 52.dp)

            HomeShortcutRow(
                icon = Icons.Default.Headphones,
                title = "Music Library",
                subtitle = "Offline cached & remote streaming audio",
                accentColor = StreamerGreen,
                onClick = onNavigateToMusic
            )

            InsetGlassDivider(startIndent = 52.dp)

            HomeShortcutRow(
                icon = Icons.Default.Storage,
                title = "Storage & Token Hub",
                subtitle = "Disk diagnostics, volume mounts & session tokens",
                accentColor = accentColor,
                onClick = onNavigateToStorage
            )

            InsetGlassDivider(startIndent = 52.dp)

            HomeShortcutRow(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Appearance, accents, themes & server links",
                accentColor = TextSecondary,
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun HomeShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(15.dp)
        )
    }
}
