package com.codershubinc.aaxion_mob.feature.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.data.OmdbMediaItem
import com.codershubinc.aaxion_mob.core.data.PosterCacheManager
import com.codershubinc.aaxion_mob.core.data.VideoPlaybackMeta
import com.codershubinc.aaxion_mob.core.media.MediaTitleParser
import com.codershubinc.aaxion_mob.core.media.RecentMediaItem
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.ui.designsystem.GlassCard
import com.codershubinc.aaxion_mob.ui.designsystem.formatTimeMs
import com.codershubinc.aaxion_mob.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentMediaGlassCard(
    item: RecentMediaItem,
    serverUri: String,
    token: String,
    omdbItem: OmdbMediaItem?,
    videoMeta: VideoPlaybackMeta?,
    isPlayingInternal: Boolean,
    isPlayingInVlc: Boolean,
    livePositionMs: Long? = null,
    liveDurationMs: Long? = null,
    onPlayClick: () -> Unit,
    onInspectOrActionClick: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActiveSession = isPlayingInternal || isPlayingInVlc
    val mediaTint = if (item.isVideo) StreamerOrange else StreamerGreen

    val parsedInfo = remember(item.rawPath, item.title) {
        MediaTitleParser.parse(item.rawPath.substringAfterLast('/'))
    }

    val displayTitle = remember(omdbItem, parsedInfo, item.title) {
        omdbItem?.title?.takeIf { it.isNotBlank() } ?: parsedInfo.cleanTitle.ifBlank { item.title }
    }

    // 1. Check metadata poster, 2. Resolve via local disk cache, 3. Fall back to server thumbnail for video
    val candidateArtworkUrl = remember(omdbItem, item.artworkUrl) {
        omdbItem?.posterUrl?.takeIf { omdbItem.hasPoster } ?: item.artworkUrl
    }
    val resolvedArtworkSource = remember(candidateArtworkUrl, item.isVideo, serverUri, token, item.rawPath, context) {
        if (!candidateArtworkUrl.isNullOrBlank()) {
            PosterCacheManager.getInstance(context).resolvePosterSource(candidateArtworkUrl)
        } else if (item.isVideo) {
            AaxionUrlBuilder.buildThumbUrl(serverUri, item.rawPath, token)
        } else null
    }

    val pulseAlpha = if (isActiveSession) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_playing")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else 1.0f

    val effectivePos = livePositionMs ?: videoMeta?.positionMs?.takeIf { it > 0L } ?: item.progressMs
    val effectiveDur = liveDurationMs ?: videoMeta?.durationMs?.takeIf { it > 0L } ?: item.durationMs

    val progressFraction = if (effectiveDur > 0L) {
        (effectivePos.toFloat() / effectiveDur.toFloat()).coerceIn(0f, 1f)
    } else if (effectivePos > 0L) {
        0.08f
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "recent_media_progress"
    )

    val remainingMs = if (effectiveDur > effectivePos) effectiveDur - effectivePos else 0L
    val percent = if (effectiveDur > 0L) ((effectivePos * 100) / effectiveDur).toInt().coerceIn(0, 100) else null

    val leftText = when {
        isActiveSession && effectivePos > 0L -> "Playing • ${formatTimeMs(effectivePos)}"
        remainingMs > 0L -> "${formatTimeMs(remainingMs)} left"
        effectivePos > 0L -> "At ${formatTimeMs(effectivePos)}"
        else -> "Not started"
    }
    val rightText = when {
        percent != null -> "$percent%"
        else -> item.subtitle.ifBlank { "" }
    }

    GlassCard(
        modifier = modifier
            .width(308.dp)
            .height(118.dp)
            .drawBehind {
                if (isActiveSession) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(mediaTint.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.18f, size.height * 0.5f),
                            radius = size.maxDimension * 0.72f
                        )
                    )
                }
            },
        cornerRadius = 24.dp,
        accentHighlight = mediaTint.copy(alpha = if (isActiveSession) 0.65f else 0.25f),
        isHighlighted = isActiveSession,
        contentPadding = PaddingValues(0.dp),
        onClick = onPlayClick,
        onLongClick = onInspectOrActionClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Background Artwork Backdrop spanning the full tile with progressive blur (0 to <10dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                if (resolvedArtworkSource != null) {
                    // Layer 1: Base sharp artwork spanning the full tile (blur = 0)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedArtworkSource)
                            .crossfade(true)
                            .size(480)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Layer 2: Progressive subtle blur overlay ramping gently from 0 on the left to 2.dp on the right
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(resolvedArtworkSource)
                            .crossfade(true)
                            .size(240)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(2.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                // Alpha mask: 0% blur on the left (sharp), gently increasing across to subtle blur on the right
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0.0f to Color.Transparent,
                                        0.20f to Color.Transparent,
                                        0.65f to Color.Black,
                                        1.0f to Color.Black
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            },
                        contentScale = ContentScale.Crop
                    )

                    // Layer 3: Gentle horizontal glass wash allowing the artwork to shine through vibrantly
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.10f),
                                    0.22f to Color.Black.copy(alpha = 0.20f),
                                    0.50f to Color(0x60090912),
                                    0.78f to Color(0x8A090912),
                                    1.0f to Color(0xB5090912)
                                )
                            )
                    )

                    // Layer 4: Vertical vignette for top and bottom edge depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = 0.45f),
                                    0.35f to Color.Transparent,
                                    0.65f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.70f)
                                )
                            )
                    )
                } else {
                    // Themed ambient fallback when no artwork is present
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        mediaTint.copy(alpha = 0.22f),
                                        mediaTint.copy(alpha = 0.08f),
                                        Color(0xFF090912)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(
                            imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = mediaTint.copy(alpha = 0.30f),
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .size(44.dp)
                                .blur(2.dp)
                        )
                    }
                }
            }

            // Foreground Content Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Floating Action Zone (anchored over the sharpest poster area)
                Box(
                    modifier = Modifier
                        .width(66.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Season/Episode Badge (top-left)
                    if (parsedInfo.seasonEpisodeBadge != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(0.6.dp, StreamerPurple.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = parsedInfo.seasonEpisodeBadge!!,
                                color = Color(0xFFD4BFFF),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Floating Glass Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.70f))
                            .border(1.2.dp, mediaTint, CircleShape)
                            .clickable(onClick = onPlayClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActiveSession) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isActiveSession) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Metadata Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. Top Badges & Remove Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            val statusPillText = when {
                                isActiveSession -> "LIVE"
                                parsedInfo.isSeries || omdbItem?.type.equals("series", ignoreCase = true) -> "SERIES"
                                item.isVideo -> "MOVIE"
                                else -> "MUSIC"
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(
                                        0.5.dp,
                                        if (isActiveSession) mediaTint.copy(alpha = pulseAlpha) else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isActiveSession) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                            .background(mediaTint.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = statusPillText,
                                        color = if (isActiveSession) mediaTint else Color.White.copy(alpha = 0.85f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    )
                                }
                            }

                            if (videoMeta?.resolutionLabel?.isNotEmpty() == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = videoMeta.resolutionLabel,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (omdbItem?.hasRating == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .border(0.5.dp, AaxionWarning.copy(alpha = 0.4f), RoundedCornerShape(50))
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "★ ${omdbItem.rating}",
                                        color = AaxionWarning,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 24.dp circular Close button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(onClick = onRemove),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // 2. Title & Context Metadata
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = displayTitle,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    offset = Offset(0f, 1.5f),
                                    blurRadius = 4f
                                )
                            )
                        )

                        val parentDir = remember(item.rawPath) {
                            val dir = item.rawPath.substringBeforeLast('/', "")
                            if (dir.isBlank()) "/" else dir
                        }
                        val parentDirName = remember(parentDir) {
                            parentDir.substringAfterLast('/').ifBlank { "Root" }
                        }

                        val subtitleDetail = when {
                            parsedInfo.isSeries && !parsedInfo.episodeTitle.isNullOrBlank() -> {
                                "${parsedInfo.seasonBadgeLong ?: "Series"} • ${parsedInfo.episodeTitle}"
                            }
                            omdbItem != null && (omdbItem.year.isNotBlank() || omdbItem.genre.isNotBlank()) -> {
                                val parts = listOfNotNull(
                                    omdbItem.year.takeIf { it.isNotBlank() },
                                    omdbItem.genre.substringBefore(',').trim().takeIf { it.isNotBlank() && it != "N/A" }
                                )
                                parts.joinToString(" • ")
                            }
                            else -> null
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E1E28).copy(alpha = 0.72f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .clickable { onOpenFolder(parentDir) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (subtitleDetail != null) {
                                Text(
                                    text = subtitleDetail,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = parentDirName,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 3. Progress Bar & Footer Row
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(mediaTint.copy(alpha = 0.65f), mediaTint)
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF1E1E28).copy(alpha = 0.72f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = leftText,
                                    color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (rightText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF1E1E28).copy(alpha = 0.72f))
                                        .border(
                                            0.5.dp,
                                            if (isActiveSession) mediaTint.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
                                            RoundedCornerShape(5.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = rightText,
                                        color = if (isActiveSession) mediaTint else Color.White.copy(alpha = 0.90f),
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isActiveSession) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
