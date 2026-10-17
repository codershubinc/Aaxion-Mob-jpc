package com.codershubinc.aaxion_mob.feature.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.data.VideoMetadataCacheDb
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.RecentMediaItem
import com.codershubinc.aaxion_mob.core.media.RecentPlaybackManager
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    serverUri: String,
    token: String,
    onNavigateToDrive: (path: String?) -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    isServerOnline: Boolean?,
    serverLatencyMs: Long?,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    val recentManager = remember { RecentPlaybackManager.getInstance(context) }
    val recentItems by recentManager.recentItems.collectAsState()

    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoadingStorage by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(serverUri, token, refreshTrigger) {
        if (serverUri.isNotBlank() && token.isNotBlank()) {
            isLoadingStorage = true
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = tokenManager.withAuthRetry(serverUri, token) { auth ->
                    service.getStorageInfo("Bearer $auth")
                }
                if (response.isSuccessful) {
                    storageInfo = response.body()
                }
            } catch (_: Exception) {
            } finally {
                isLoadingStorage = false
            }
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Late Night Session"
        }
    }

    val displayHost = remember(serverUri) {
        try {
            serverUri.toUri().host ?: serverUri.ifBlank { "No Host Configured" }
        } catch (_: Exception) {
            serverUri.ifBlank { "No Host Configured" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (isTablet) 24.dp else 16.dp,
                end = if (isTablet) 24.dp else 16.dp,
                top = 14.dp,
                bottom = 110.dp
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Top Command & Telemetry Header
        HomeHeaderBar(
            greeting = greeting,
            displayHost = displayHost,
            isServerOnline = isServerOnline,
            serverLatencyMs = serverLatencyMs,
            accentColor = accentColor,
            onNavigateToConnectionManager = onNavigateToConnectionManager,
            onNavigateToSettings = onNavigateToSettings,
            onRefresh = { refreshTrigger++ }
        )

        // 2. Hero Storage & Volume Telemetry Card
        StorageHeroCard(
            storageInfo = storageInfo,
            isLoading = isLoadingStorage,
            accentColor = accentColor,
            onOpenStorageHub = onNavigateToStorage,
            onSelectMountPoint = { path -> onNavigateToDrive(path) }
        )

        // 3. Spatial Bento Quick-Access Grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(
                title = "WORKSPACES",
                actionText = "Browse Root",
                accentColor = accentColor,
                onActionClick = { onNavigateToDrive("/") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoWorkspaceTile(
                    title = "My Drive",
                    subtitle = "Explore host filesystem",
                    badge = "FILES",
                    icon = Icons.Default.Folder,
                    tint = accentColor,
                    onClick = { onNavigateToDrive(null) },
                    modifier = Modifier.weight(1f)
                )
                BentoWorkspaceTile(
                    title = "Music Library",
                    subtitle = "Stream audio & playlists",
                    badge = "AUDIO",
                    icon = Icons.Default.Headphones,
                    tint = StreamerGreen,
                    onClick = onNavigateToMusic,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoWorkspaceTile(
                    title = "Storage & Hub",
                    subtitle = "Volumes & auth tokens",
                    badge = "SYSTEM",
                    icon = Icons.Default.Storage,
                    tint = StreamerPurple,
                    onClick = onNavigateToStorage,
                    modifier = Modifier.weight(1f)
                )
                BentoWorkspaceTile(
                    title = "Network & Host",
                    subtitle = displayHost,
                    badge = if (isServerOnline == true) "ACTIVE" else "SETUP",
                    icon = Icons.Default.Dns,
                    tint = StreamerOrange,
                    onClick = onNavigateToConnectionManager,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Continue Playing / Recent Media Stream
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(
                title = "CONTINUE PLAYING",
                actionText = if (recentItems.isNotEmpty()) "Clear" else null,
                accentColor = accentColor,
                onActionClick = { recentManager.clearAll() }
            )

            if (recentItems.isEmpty()) {
                InsetGroupedGlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.5.dp, GlassHairline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Recent Media",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Videos and music tracks you play will appear here for instant resume.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(recentItems.take(12), key = { it.rawPath }) { item ->
                        val fileItem = item.toFileItem()
                        val streamUrl = AaxionUrlBuilder.buildStreamUrl(serverUri, item.rawPath, token)
                        RecentMediaGlassCard(
                            item = item,
                            onClick = {
                                if (item.isVideo) {
                                    VlcPlayerManager.getInstance(context).launchVlc(
                                        context = context,
                                        file = fileItem,
                                        streamUrl = streamUrl,
                                        resume = item.progressMs >= 5000L,
                                        token = token
                                    )
                                } else {
                                    MediaPlayerManager.getInstance(context).play(
                                        file = fileItem,
                                        streamUrl = streamUrl,
                                        isVideo = false,
                                        token = token,
                                        baseUrl = serverUri,
                                        artworkUrlOverride = item.artworkUrl,
                                        displayTitle = item.title,
                                        displayArtist = item.subtitle
                                    )
                                }
                            },
                            onRemove = { recentManager.removeItem(item.rawPath) },
                            onInspect = if (item.isVideo) {
                                {
                                    VlcPlayerManager.getInstance(context).inspectVideo(
                                        file = fileItem,
                                        streamUrl = streamUrl,
                                        token = token
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderBar(
    greeting: String,
    displayHost: String,
    isServerOnline: Boolean?,
    serverLatencyMs: Long?,
    accentColor: Color,
    onNavigateToConnectionManager: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val statusColor = when (isServerOnline) {
        true -> AaxionSuccess
        false -> AaxionDanger
        null -> TextTertiary
    }
    val statusLabel = when (isServerOnline) {
        true -> if (serverLatencyMs != null) "${serverLatencyMs}ms" else "Online"
        false -> "Offline"
        null -> "Checking..."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting.uppercase(Locale.US),
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Aaxion Hub",
                color = TextPrimary,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Server Status Capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassSurfaceMedium)
                    .border(0.75.dp, GlassBrushes.specularBorder(statusColor.copy(alpha = 0.4f)), RoundedCornerShape(50))
                    .clickable(onClick = onNavigateToConnectionManager)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayHost,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPillIconButton(
                onClick = onRefresh,
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh Telemetry",
                size = 38.dp,
                iconSize = 18.dp,
                tint = TextSecondary,
                accentHighlight = accentColor
            )
            GlassPillIconButton(
                onClick = onNavigateToSettings,
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                size = 38.dp,
                iconSize = 18.dp,
                tint = TextPrimary,
                accentHighlight = accentColor
            )
        }
    }
}

@Composable
private fun StorageHeroCard(
    storageInfo: StorageInfoResponse?,
    isLoading: Boolean,
    accentColor: Color,
    onOpenStorageHub: () -> Unit,
    onSelectMountPoint: (String) -> Unit
) {
    val usagePct = storageInfo?.usagePercentage ?: 0.0
    val rawProgress = (usagePct / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "hero_storage_bar"
    )

    val healthTint = when {
        rawProgress >= 0.90f -> AaxionDanger
        rawProgress >= 0.75f -> AaxionWarning
        else -> accentColor
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        accentHighlight = healthTint,
        isHighlighted = true,
        contentPadding = PaddingValues(18.dp),
        onClick = onOpenStorageHub
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(healthTint.copy(alpha = 0.18f))
                        .border(0.75.dp, healthTint.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = healthTint,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "System Storage",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (storageInfo != null) {
                            "${IntentUtils.formatFileSize(storageInfo.available)} available of ${IntentUtils.formatFileSize(storageInfo.total)}"
                        } else if (isLoading) {
                            "Syncing volume telemetry..."
                        } else {
                            "Tap to view storage details"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (storageInfo != null) {
                Text(
                    text = String.format(Locale.US, "%.0f%%", storageInfo.usagePercentage),
                    color = healthTint,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            } else if (isLoading) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Capsule Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(healthTint.copy(alpha = 0.65f), healthTint)
                        )
                    )
            )
        }

        // Mounted Volume Quick-Jump Pills
        if (storageInfo != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VolumeQuickPill(
                    label = "Host (/)",
                    usedPct = String.format(Locale.US, "%.0f%%", storageInfo.usagePercentage),
                    icon = Icons.Default.Folder,
                    tint = accentColor,
                    onClick = { onSelectMountPoint("/") }
                )

                storageInfo.externalDevices?.take(2)?.forEach { ext ->
                    val name = ext.device.substringAfterLast('/').ifBlank { "USB" }
                    VolumeQuickPill(
                        label = name,
                        usedPct = String.format(Locale.US, "%.0f%%", ext.usagePercentage),
                        icon = Icons.Default.Usb,
                        tint = StreamerOrange,
                        onClick = { onSelectMountPoint(ext.mountPoint.ifBlank { "/" }) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeQuickPill(
    label: String,
    usedPct: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.6.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = usedPct,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BentoWorkspaceTile(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(118.dp),
        cornerRadius = 20.dp,
        accentHighlight = tint,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.2f),
                            radius = size.maxDimension * 0.65f
                        )
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.copy(alpha = 0.16f))
                        .border(0.6.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tint.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = tint,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentMediaGlassCard(
    item: RecentMediaItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onInspect: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val mediaTint = if (item.isVideo) StreamerOrange else StreamerGreen
    val cachedMeta = if (item.isVideo) {
        remember(item.rawPath) { VideoMetadataCacheDb.getInstance(context).get(item.rawPath) }
    } else null

    val progressFraction = if (item.durationMs > 0) {
        (item.progressMs.toFloat() / item.durationMs).coerceIn(0.05f, 1f)
    } else 0f

    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(168.dp)
            .clip(shape)
            .background(GlassBrushes.glassSurface(accent = mediaTint))
            .border(0.75.dp, GlassBrushes.specularBorder(mediaTint.copy(alpha = 0.35f)), shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (onInspect != null) {
                        onInspect()
                    } else {
                        onRemove()
                    }
                }
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Artwork / Media Preview Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0E0E14))
                .border(0.5.dp, GlassHairline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.artworkUrl)
                        .crossfade(true)
                        .size(240)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = mediaTint.copy(alpha = 0.75f),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Floating Play Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.75.dp, mediaTint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = mediaTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Progress Bar along bottom edge of thumbnail
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .background(mediaTint)
                    )
                }
            }
        }

        Column {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subText = when {
                    item.formattedProgress.isNotBlank() -> "At ${item.formattedProgress}"
                    cachedMeta != null && cachedMeta.formattedBitrate.isNotEmpty() -> cachedMeta.formattedBitrate
                    else -> item.subtitle
                }
                Text(
                    text = subText,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val badgeText = when {
                    item.isVideo && cachedMeta != null && cachedMeta.resolutionLabel.isNotEmpty() -> cachedMeta.resolutionLabel
                    item.isVideo -> "VID"
                    else -> "AUD"
                }
                Text(
                    text = badgeText,
                    color = mediaTint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    accentColor: Color,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}