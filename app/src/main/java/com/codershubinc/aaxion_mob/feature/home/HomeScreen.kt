package com.codershubinc.aaxion_mob.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.RecentPlaybackManager
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Calendar

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
    val vlcManager = remember { VlcPlayerManager.getInstance(context) }
    val mediaManager = remember { MediaPlayerManager.getInstance(context) }
    val recentItems by recentManager.recentItems.collectAsState()
    val activeVlcSession by vlcManager.activeSession.collectAsState()
    val activeMediaState by mediaManager.state.collectAsState()

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
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(recentItems.take(12), key = { it.rawPath }) { item ->
                        val fileItem = item.toFileItem()
                        val streamUrl = AaxionUrlBuilder.buildStreamUrl(serverUri, item.rawPath, token)

                        val isVlcPlayingThis = activeVlcSession?.rawPath == item.rawPath && activeVlcSession?.isPlayingInVlc == true
                        val isMediaPlayingThis = activeMediaState.isPlaying && activeMediaState.currentFile?.rawPath == item.rawPath
                        val isCurrentlyPlaying = isVlcPlayingThis || isMediaPlayingThis

                        val livePositionMs = when {
                            isVlcPlayingThis -> activeVlcSession?.meta?.positionMs
                            isMediaPlayingThis -> activeMediaState.currentPositionMs
                            else -> null
                        }
                        val liveDurationMs = when {
                            isVlcPlayingThis -> activeVlcSession?.meta?.durationMs?.takeIf { it > 0L }
                            isMediaPlayingThis -> activeMediaState.durationMs.takeIf { it > 0L }
                            else -> null
                        }

                        RecentMediaGlassCard(
                            item = item,
                            isCurrentlyPlaying = isCurrentlyPlaying,
                            livePositionMs = livePositionMs,
                            liveDurationMs = liveDurationMs,
                            onClick = {
                                if (item.isVideo) {
                                    vlcManager.launchVlc(
                                        context = context,
                                        file = fileItem,
                                        streamUrl = streamUrl,
                                        resume = (livePositionMs ?: item.progressMs) >= 5000L,
                                        token = token
                                    )
                                } else {
                                    mediaManager.play(
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
                                    vlcManager.inspectVideo(
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