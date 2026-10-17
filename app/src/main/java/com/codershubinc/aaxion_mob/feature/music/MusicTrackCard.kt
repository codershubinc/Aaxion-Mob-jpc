package com.codershubinc.aaxion_mob.feature.music

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.codershubinc.aaxion_mob.core.media.ArtworkResolver
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun MusicTrackCard(
    track: FileItem,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    siblingFiles: List<FileItem>,
    serverUri: String,
    token: String,
    onClick: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artworkUrl = ArtworkResolver.findArtworkUrl(track, siblingFiles, serverUri, token)

    val cleanTitle = track.nameWithoutExtension.ifBlank { track.name }
    val formatBadge = track.extension.uppercase().ifBlank { "AUDIO" }

    // Pulsing border highlight for actively playing track
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_border")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardShape = RoundedCornerShape(14.dp)
    val cardBorderColor = if (isCurrentTrack) {
        StreamerGreen.copy(alpha = if (isPlaying) pulseAlpha else 0.45f)
    } else {
        GlassHairline
    }

    val cardBg = if (isCurrentTrack) {
        StreamerGreen.copy(alpha = 0.08f)
    } else {
        AaxionCardBg
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(if (isCurrentTrack) 1.2.dp else 0.5.dp, cardBorderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art or Audio waveform emblem
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        0.8.dp,
                        if (isCurrentTrack) StreamerGreen.copy(alpha = 0.6f) else GlassHairline,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = cleanTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isCurrentTrack && isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrentTrack) StreamerGreen else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // If currently playing, show miniature equalizer badge overlay
                if (isCurrentTrack && isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(StreamerGreen.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track metadata: Title, format badge, and human-readable size
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = cleanTitle,
                    color = if (isCurrentTrack) StreamerGreen else TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Format badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrentTrack) StreamerGreen.copy(alpha = 0.2f) else GlassSurfaceMedium)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = formatBadge,
                            color = if (isCurrentTrack) StreamerGreen else TextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = IntentUtils.formatFileSize(track.size),
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Play/Pause Action Button
            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack && isPlaying) {
                            StreamerGreen.copy(alpha = 0.2f)
                        } else {
                            GlassSurfaceMedium
                        }
                    )
            ) {
                Icon(
                    imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentTrack && isPlaying) "Pause" else "Play",
                    tint = if (isCurrentTrack) StreamerGreen else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
