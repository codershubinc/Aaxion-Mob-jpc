package com.codershubinc.aaxion_mob.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.data.VideoMetadataCacheDb
import com.codershubinc.aaxion_mob.core.media.RecentMediaItem
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.formatTimeMs
import com.codershubinc.aaxion_mob.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecentMediaGlassCard(
    item: RecentMediaItem,
    isCurrentlyPlaying: Boolean = false,
    livePositionMs: Long? = null,
    liveDurationMs: Long? = null,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onInspect: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val mediaTint = if (item.isVideo) StreamerOrange else StreamerGreen
    val cachedMeta = if (item.isVideo) {
        remember(item.rawPath) { VideoMetadataCacheDb.getInstance(context).get(item.rawPath) }
    } else null

    val effectivePositionMs = livePositionMs ?: cachedMeta?.positionMs?.takeIf { it > 0L } ?: item.progressMs
    val effectiveDurationMs = liveDurationMs ?: cachedMeta?.durationMs?.takeIf { it > 0L } ?: item.durationMs

    val progressFraction = if (effectiveDurationMs > 0L) {
        (effectivePositionMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else if (effectivePositionMs > 0L) {
        0.1f
    } else 0f

    val percentWatched = if (effectiveDurationMs > 0L) {
        ((effectivePositionMs * 100) / effectiveDurationMs).toInt().coerceIn(0, 100)
    } else null

    val elapsedText = when {
        effectiveDurationMs > 0L -> "${formatTimeMs(effectivePositionMs)} / ${formatTimeMs(effectiveDurationMs)}"
        effectivePositionMs > 0L -> "At ${formatTimeMs(effectivePositionMs)}"
        else -> "Not started"
    }

    val remainingMs = if (effectiveDurationMs > effectivePositionMs) {
        effectiveDurationMs - effectivePositionMs
    } else null

    val remainingText = when {
        remainingMs != null && remainingMs > 0L -> "${formatTimeMs(remainingMs)} left"
        effectivePositionMs > 0L && percentWatched == 100 -> "Completed"
        cachedMeta != null && cachedMeta.fileSize > 0L -> IntentUtils.formatFileSize(cachedMeta.fileSize)
        item.size > 0L -> IntentUtils.formatFileSize(item.size)
        else -> null
    }

    val streamSpecText = when {
        item.isVideo && cachedMeta != null && cachedMeta.formattedBitrate.isNotBlank() -> cachedMeta.formattedBitrate
        item.isVideo && cachedMeta != null && cachedMeta.resolutionLabel.isNotBlank() -> cachedMeta.resolutionLabel
        item.isVideo -> "VIDEO"
        item.subtitle.isNotBlank() -> item.subtitle
        else -> "AUDIO"
    }

    val relativeTime = remember(item.lastPlayedTs) {
        formatRelativeTime(item.lastPlayedTs)
    }

    val pulseAlpha = if (isCurrentlyPlaying) {
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

    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(184.dp)
            .clip(shape)
            .background(GlassBrushes.glassSurface(accent = mediaTint, isHighlighted = isCurrentlyPlaying))
            .border(
                width = if (isCurrentlyPlaying) 1.dp else 0.75.dp,
                brush = GlassBrushes.specularBorder(mediaTint.copy(alpha = if (isCurrentlyPlaying) 0.6f else 0.3f)),
                shape = shape
            )
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
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thumbnail & Overlay Badges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF0C0C12))
                .border(0.5.dp, GlassHairline, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.artworkUrl)
                        .crossfade(true)
                        .size(260)
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
                    modifier = Modifier.size(34.dp)
                )
            }

            // Top and Bottom Ambient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            // Top Status Badges Row
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentlyPlaying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(0.6.dp, mediaTint.copy(alpha = pulseAlpha), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(mediaTint.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAYING",
                            color = mediaTint,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    val pillText = when {
                        item.isVideo && cachedMeta != null && cachedMeta.resolutionLabel.isNotBlank() -> cachedMeta.resolutionLabel
                        item.isVideo -> "VIDEO"
                        else -> "AUDIO"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pillText,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                if (relativeTime.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = relativeTime,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Floating Play / Pause Action Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(0.75.dp, if (isCurrentlyPlaying) mediaTint else Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentlyPlaying) "Playing" else "Resume",
                    tint = if (isCurrentlyPlaying) mediaTint else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Progress Bar pinned to bottom edge
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(mediaTint.copy(alpha = 0.75f), mediaTint)
                                )
                            )
                    )
                }
            }
        }

        // Info Text Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Elapsed / Duration & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isCurrentlyPlaying) mediaTint else TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = elapsedText,
                        color = if (isCurrentlyPlaying) TextPrimary else TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = if (isCurrentlyPlaying) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (percentWatched != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$percentWatched%",
                        color = mediaTint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Remaining Time & Stream Spec
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = remainingText ?: item.subtitle.ifBlank { "Ready to resume" },
                    color = TextTertiary,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = streamSpecText,
                    color = mediaTint.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

internal fun formatRelativeTime(ts: Long): String {
    if (ts <= 0L) return ""
    val diffMs = (System.currentTimeMillis() - ts).coerceAtLeast(0L)
    val diffSec = diffMs / 1000
    val diffMin = diffSec / 60
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24
    return when {
        diffSec < 60 -> "Just now"
        diffMin < 60 -> "${diffMin}m ago"
        diffHours < 24 -> "${diffHours}h ago"
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "${diffDays}d ago"
        else -> "${diffDays / 7}w ago"
    }
}
