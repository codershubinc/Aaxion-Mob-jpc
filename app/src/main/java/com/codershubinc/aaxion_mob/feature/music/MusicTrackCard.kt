package com.codershubinc.aaxion_mob.feature.music

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
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
import com.codershubinc.aaxion_mob.core.media.ArtworkResolver
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.MusicTrack
import com.codershubinc.aaxion_mob.core.network.buildArtworkUrl
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
    val artworkUrl = remember(track.rawPath, siblingFiles) {
        ArtworkResolver.findArtworkUrl(track, siblingFiles, serverUri, token)
    }

    // Dynamic accent color extracted from album artwork
    val cachedAccent = remember(artworkUrl) {
        ArtworkColorExtractor.getCachedColor(artworkUrl)
    }
    var dynamicAccent by remember(artworkUrl) {
        mutableStateOf(cachedAccent ?: StreamerGreen)
    }

    val animatedAccent by animateColorAsState(
        targetValue = dynamicAccent,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "card_artwork_dynamic_accent"
    )

    val cleanTitle = track.nameWithoutExtension.ifBlank { track.name }
    val formatBadge = track.extension.uppercase().ifBlank { "AUDIO" }

    // Pulsing glow border highlight for actively playing track
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_border")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardShape = RoundedCornerShape(14.dp)
    val cardBorderColor = if (isCurrentTrack) {
        animatedAccent.copy(alpha = if (isPlaying) pulseAlpha else 0.55f)
    } else {
        animatedAccent.copy(alpha = 0.16f)
    }

    val cardBg = if (isCurrentTrack) {
        Brush.horizontalGradient(
            listOf(
                animatedAccent.copy(alpha = 0.15f),
                AaxionCardBg
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                AaxionCardBg,
                AaxionCardBg
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(if (isCurrentTrack) 1.4.dp else 0.6.dp, cardBorderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art or Audio waveform emblem with dynamic border glow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        1.dp,
                        animatedAccent.copy(alpha = if (isCurrentTrack) 0.85f else 0.40f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .allowHardware(false)
                            .crossfade(true)
                            .build(),
                        contentDescription = cleanTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            if (drawable is BitmapDrawable) {
                                val extracted = ArtworkColorExtractor.extractAccent(
                                    bitmap = drawable.bitmap,
                                    cacheKey = artworkUrl,
                                    fallback = StreamerGreen
                                )
                                dynamicAccent = extracted
                            }
                        }
                    )
                } else {
                    Icon(
                        imageVector = if (isCurrentTrack && isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrentTrack) animatedAccent else animatedAccent.copy(alpha = 0.75f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // If currently playing, show miniature equalizer badge overlay in dynamic color
                if (isCurrentTrack && isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(animatedAccent.copy(alpha = 0.95f)),
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

            // Track metadata: Title, dynamic format badge, and size
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = cleanTitle,
                    color = if (isCurrentTrack) animatedAccent else TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dynamic Format badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(animatedAccent.copy(alpha = 0.18f))
                            .border(0.5.dp, animatedAccent.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = formatBadge,
                            color = animatedAccent,
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

            // Quick Play/Pause Action Button with dynamic accent
            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack && isPlaying) {
                            animatedAccent.copy(alpha = 0.25f)
                        } else {
                            GlassSurfaceMedium
                        }
                    )
                    .border(
                        0.6.dp,
                        if (isCurrentTrack) animatedAccent.copy(alpha = 0.5f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentTrack && isPlaying) "Pause" else "Play",
                    tint = if (isCurrentTrack) animatedAccent else animatedAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MusicTrackCard(
    track: MusicTrack,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    serverUri: String,
    token: String,
    onClick: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artworkUrl = remember(track.imagePath, serverUri, token) {
        if (track.imagePath.isNotBlank()) track.buildArtworkUrl(serverUri, token) else null
    }

    // Dynamic accent color extracted from album artwork
    val cachedAccent = remember(artworkUrl) {
        ArtworkColorExtractor.getCachedColor(artworkUrl)
    }
    var dynamicAccent by remember(artworkUrl) {
        mutableStateOf(cachedAccent ?: StreamerGreen)
    }

    val animatedAccent by animateColorAsState(
        targetValue = dynamicAccent,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "track_artwork_dynamic_accent"
    )

    // Pulsing glow border highlight for actively playing track
    val infiniteTransition = rememberInfiniteTransition(label = "music_track_pulse_border")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "music_track_pulse_alpha"
    )

    val cardShape = RoundedCornerShape(14.dp)
    val cardBorderColor = if (isCurrentTrack) {
        animatedAccent.copy(alpha = if (isPlaying) pulseAlpha else 0.55f)
    } else {
        animatedAccent.copy(alpha = 0.16f)
    }

    val cardBg = if (isCurrentTrack) {
        Brush.horizontalGradient(
            listOf(
                animatedAccent.copy(alpha = 0.15f),
                AaxionCardBg
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                AaxionCardBg,
                AaxionCardBg
            )
        )
    }

    val formatBadge = track.filePath.substringAfterLast('.', "MP3").uppercase()
    val durSec = track.duration.toInt()
    val durationText = if (durSec > 0) {
        String.format(java.util.Locale.US, "%d:%02d", durSec / 60, durSec % 60)
    } else ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(if (isCurrentTrack) 1.4.dp else 0.6.dp, cardBorderColor, cardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art with dynamic border glow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        1.dp,
                        animatedAccent.copy(alpha = if (isCurrentTrack) 0.85f else 0.40f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!artworkUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .allowHardware(false)
                            .crossfade(true)
                            .build(),
                        contentDescription = track.displayTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            if (drawable is BitmapDrawable) {
                                val extracted = ArtworkColorExtractor.extractAccent(
                                    bitmap = drawable.bitmap,
                                    cacheKey = artworkUrl,
                                    fallback = StreamerGreen
                                )
                                dynamicAccent = extracted
                            }
                        }
                    )
                } else {
                    Icon(
                        imageVector = if (isCurrentTrack && isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrentTrack) animatedAccent else animatedAccent.copy(alpha = 0.75f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // If currently playing, show miniature equalizer badge overlay in dynamic color
                if (isCurrentTrack && isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(animatedAccent.copy(alpha = 0.95f)),
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

            // Track metadata: Title, Artist & Album, Format & Duration
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = track.displayTitle,
                    color = if (isCurrentTrack) animatedAccent else TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = buildString {
                        append(track.displayArtist)
                        if (track.album.isNotBlank() && track.album != track.title) {
                            append(" • ")
                            append(track.album)
                        }
                    },
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dynamic Format badge pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(animatedAccent.copy(alpha = 0.18f))
                            .border(0.5.dp, animatedAccent.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = formatBadge,
                            color = animatedAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (durationText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = durationText,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }

                    if (track.releaseYear > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = track.releaseYear.toString(),
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick Play/Pause Action Button with dynamic accent
            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentTrack && isPlaying) {
                            animatedAccent.copy(alpha = 0.25f)
                        } else {
                            GlassSurfaceMedium
                        }
                    )
                    .border(
                        0.6.dp,
                        if (isCurrentTrack) animatedAccent.copy(alpha = 0.5f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isCurrentTrack && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentTrack && isPlaying) "Pause" else "Play",
                    tint = if (isCurrentTrack) animatedAccent else animatedAccent.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

