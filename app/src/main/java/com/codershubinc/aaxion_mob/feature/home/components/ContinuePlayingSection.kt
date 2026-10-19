package com.codershubinc.aaxion_mob.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
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
import com.codershubinc.aaxion_mob.core.data.OmdbMetadataCacheDb
import com.codershubinc.aaxion_mob.core.data.VideoMetadataCacheDb
import com.codershubinc.aaxion_mob.core.media.MediaTitleParser
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.RecentPlaybackManager
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.core.network.OmdbClient
import com.codershubinc.aaxion_mob.ui.designsystem.InsetGroupedGlassCard
import com.codershubinc.aaxion_mob.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

enum class RecentMediaFilter(val label: String) {
    ALL("All"),
    VIDEO("Videos"),
    AUDIO("Music")
}

@Composable
fun ContinuePlayingSection(
    serverUri: String,
    token: String,
    accentColor: Color,
    onNavigateToDrive: (path: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentManager = remember { RecentPlaybackManager.getInstance(context) }
    val recentItems by recentManager.recentItems.collectAsState()
    val playerManager = remember { MediaPlayerManager.getInstance(context) }
    val playerState by playerManager.state.collectAsState()
    val vlcManager = remember { VlcPlayerManager.getInstance(context) }
    val vlcSession by vlcManager.activeSession.collectAsState()
    val omdbDb = remember { OmdbMetadataCacheDb.getInstance(context) }
    val omdbCache by omdbDb.cachedItems.collectAsState()
    val videoMetaDb = remember { VideoMetadataCacheDb.getInstance(context) }

    var selectedFilter by remember { mutableStateOf(RecentMediaFilter.ALL) }

    // Sync playback state from cache on active session updates
    LaunchedEffect(Unit, vlcSession?.meta?.positionMs, vlcSession?.isPlayingInVlc, omdbCache.size) {
        withContext(Dispatchers.IO) {
            recentManager.refreshFromCache(context)
        }
    }

    // Background prefetch for top video metadata and artwork
    LaunchedEffect(recentItems.size, serverUri) {
        if (serverUri.isNotBlank()) {
            withContext(Dispatchers.IO) {
                var anyUpdated = false
                val videoItems = recentItems.filter { it.isVideo }.take(8)
                for (item in videoItems) {
                    val parsed = MediaTitleParser.parse(item.rawPath.substringAfterLast('/'))
                    if (omdbDb.getForMedia(parsed, item.rawPath) == null) {
                        val fetched = OmdbClient.fetchMetadata(context, parsed, item.rawPath)
                        if (fetched != null) {
                            anyUpdated = true
                        }
                    }
                }
                if (anyUpdated) {
                    recentManager.refreshFromCache(context)
                }
            }
        }
    }

    // Periodic refresh loop to ensure positions update
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(15_000L)
            withContext(Dispatchers.IO) {
                recentManager.refreshFromCache(context)
            }
        }
    }

    val filteredRecentItems = remember(recentItems, selectedFilter) {
        when (selectedFilter) {
            RecentMediaFilter.VIDEO -> recentItems.filter { it.isVideo }
            RecentMediaFilter.AUDIO -> recentItems.filter { it.isAudio }
            RecentMediaFilter.ALL -> recentItems
        }
    }

    val videoCount = remember(recentItems) { recentItems.count { it.isVideo } }
    val musicCount = remember(recentItems) { recentItems.count { it.isAudio } }
    val hasBothTypes = videoCount > 0 && musicCount > 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "CONTINUE PLAYING",
            actionText = if (recentItems.isNotEmpty()) "Clear" else null,
            accentColor = accentColor,
            onActionClick = { recentManager.clearAll() }
        )

        if (hasBothTypes) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecentMediaFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        RecentMediaFilter.ALL -> recentItems.size
                        RecentMediaFilter.VIDEO -> videoCount
                        RecentMediaFilter.AUDIO -> musicCount
                    }
                    val tabTint = when (filter) {
                        RecentMediaFilter.VIDEO -> StreamerOrange
                        RecentMediaFilter.AUDIO -> StreamerGreen
                        RecentMediaFilter.ALL -> accentColor
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) tabTint.copy(alpha = 0.22f) else GlassSurfaceLow)
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) tabTint.copy(alpha = 0.6f) else GlassHairline,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = filter.label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$count",
                                color = if (isSelected) tabTint else TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (filteredRecentItems.isEmpty()) {
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
                            text = if (recentItems.isEmpty()) "No Recent Media" else "No matching items",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (recentItems.isEmpty()) {
                                "Videos and music tracks you play will appear here for instant resume."
                            } else {
                                "No items found in this tab."
                            },
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
                items(filteredRecentItems.take(12), key = { it.rawPath }) { item ->
                    val fileItem = item.toFileItem()
                    val streamUrl = AaxionUrlBuilder.buildStreamUrl(serverUri, item.rawPath, token)

                    val isPlayingInVlc = vlcSession?.rawPath == item.rawPath && vlcSession?.isPlayingInVlc == true
                    val isPlayingInternal = playerState.isPlaying && playerState.currentFile?.rawPath == item.rawPath

                    val livePositionMs = when {
                        isPlayingInVlc -> vlcSession?.meta?.positionMs
                        isPlayingInternal -> playerState.currentPositionMs
                        else -> null
                    }
                    val liveDurationMs = when {
                        isPlayingInVlc -> vlcSession?.meta?.durationMs?.takeIf { it > 0L }
                        isPlayingInternal -> playerState.durationMs.takeIf { it > 0L }
                        else -> null
                    }

                    val parsedInfo = remember(item.rawPath) {
                        MediaTitleParser.parse(item.rawPath.substringAfterLast('/'))
                    }
                    val omdbItem = remember(omdbCache, item.rawPath, parsedInfo) {
                        omdbDb.getForMedia(parsedInfo, item.rawPath)
                    }
                    val videoMeta = remember(item.rawPath, vlcSession, playerState.currentPositionMs) {
                        if (item.isVideo) videoMetaDb.get(item.rawPath) else null
                    }

                    RecentMediaGlassCard(
                        item = item,
                        serverUri = serverUri,
                        token = token,
                        omdbItem = omdbItem,
                        videoMeta = videoMeta,
                        isPlayingInternal = isPlayingInternal,
                        isPlayingInVlc = isPlayingInVlc,
                        livePositionMs = livePositionMs,
                        liveDurationMs = liveDurationMs,
                        onPlayClick = {
                            if (isPlayingInternal) {
                                playerManager.togglePlayPause()
                            } else if (item.isVideo) {
                                val resolvedTitle = omdbItem?.title?.takeIf { it.isNotBlank() } ?: parsedInfo.cleanTitle.ifBlank { item.title }
                                val posterCandidate = omdbItem?.posterUrl?.takeIf { omdbItem.hasPoster } ?: item.artworkUrl
                                vlcManager.launchVlc(
                                    context = context,
                                    file = fileItem,
                                    streamUrl = streamUrl,
                                    resume = (livePositionMs ?: item.progressMs) >= 5000L,
                                    token = token,
                                    customTitle = resolvedTitle,
                                    posterUrl = posterCandidate
                                )
                            } else {
                                if (playerState.currentFile?.rawPath == item.rawPath) {
                                    playerManager.togglePlayPause()
                                } else {
                                    playerManager.play(
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
                            }
                        },
                        onInspectOrActionClick = {
                            if (item.isVideo) {
                                vlcManager.inspectVideo(
                                    file = fileItem,
                                    streamUrl = streamUrl,
                                    token = token
                                )
                            } else {
                                val parentDir = item.rawPath.substringBeforeLast('/', "").ifBlank { "/" }
                                onNavigateToDrive(parentDir)
                            }
                        },
                        onOpenFolder = { folderPath ->
                            onNavigateToDrive(folderPath)
                        },
                        onRemove = {
                            recentManager.removeItem(item.rawPath)
                        }
                    )
                }
            }
        }
    }
}
