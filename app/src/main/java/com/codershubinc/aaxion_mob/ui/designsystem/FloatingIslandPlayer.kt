package com.codershubinc.aaxion_mob.ui.designsystem

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.MediaPlaybackState
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Locale

/**
 * Format milliseconds into standard mm:ss or hh:mm:ss string.
 */
fun formatTimeMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, min, sec)
    } else {
        String.format(Locale.US, "%02d:%02d", min, sec)
    }
}

/**
 * In-built Media Player that doubles as a Floating Island capsule / card docked at the bottom.
 * Plays audio & video continuously in the background while users explore files.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingIslandPlayer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { MediaPlayerManager.getInstance(context) }
    val state by manager.state.collectAsState()

    if (!state.isVisible || state.currentFile == null) return

    val themeAccent = LocalAccentColor.current
    val mediaAccent = if (state.isVideo) StreamerOrange else StreamerGreen

    val configuration = LocalConfiguration.current
    val isMobile = configuration.screenWidthDp < 768

    // Position above mobile bottom navigation dock when collapsed, or full bottom sheet when expanded
    val bottomMargin = if (!state.isExpanded) {
        if (isMobile) 86.dp else 16.dp
    } else {
        if (isMobile) 10.dp else 20.dp
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(250)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomMargin)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!state.isExpanded) {
                // ==========================================
                // COMPACT FLOATING ISLAND (Bottom Capsule)
                // ==========================================
                CompactIslandView(
                    state = state,
                    accentColor = mediaAccent,
                    onTogglePlay = { manager.togglePlayPause() },
                    onExpand = { manager.setExpanded(true) },
                    onClose = { manager.close() }
                )
            } else {
                // ==========================================
                // EXPANDED FLOATING ISLAND (Bottom Card)
                // ==========================================
                ExpandedIslandView(
                    state = state,
                    manager = manager,
                    accentColor = mediaAccent,
                    onCollapse = { manager.setExpanded(false) },
                    onClose = { manager.close() },
                    onOpenExternal = {
                        state.currentFile?.let { file ->
                            IntentUtils.launchStreamIntent(
                                context = context,
                                streamUrl = state.streamUrl,
                                isVideo = state.isVideo,
                                title = file.name
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactIslandView(
    state: MediaPlaybackState,
    accentColor: Color,
    onTogglePlay: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    val islandShape = RoundedCornerShape(27.dp)
    val fileName = state.displayTitle ?: state.currentFile?.name ?: "Media"
    val fileExt = state.currentFile?.extension?.uppercase() ?: ""

    Box(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 460.dp)
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                // Frosted ambient radial backlight glow
                val glowRadius = size.maxDimension * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.32f),
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    )
                )
            }
            .clip(islandShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        accentColor.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = islandShape
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -12f) {
                        onExpand()
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand
            )
    ) {
        // Frosted blur backdrop layers
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(16.dp)
                .background(accentColor.copy(alpha = 0.22f), islandShape)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E2230).copy(alpha = 0.55f),
                            Color(0xFF0F121A).copy(alpha = 0.65f)
                        )
                    ),
                    islandShape
                )
        )

        // Top specular reflection line
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.92f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.50f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Album Art Thumbnail (if found) OR Animated Audio Equalizer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (state.artworkUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(0.8.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.artworkUrl)
                                .crossfade(true)
                                .size(96)
                                .build(),
                            contentDescription = "Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (state.isPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(AaxionBg.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedAudioEqualizer(
                                    isPlaying = true,
                                    color = accentColor,
                                    barWidth = 1.5.dp,
                                    maxHeight = 7f
                                )
                            }
                        }
                    }
                } else {
                    AnimatedAudioEqualizer(
                        isPlaying = state.isPlaying,
                        color = accentColor
                    )
                }
            }

            // Center: Marquee filename & time status + format badge
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = fileName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage ?: "Playback Error",
                        color = Color(0xFFFF5252),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${formatTimeMs(state.currentPositionMs)} / ${formatTimeMs(state.durationMs)}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        if (fileExt.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = fileExt,
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (state.playbackSpeed != 1.0f) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.playbackSpeed}x",
                                color = TextTertiary,
                                fontSize = 9.sp
                            )
                        }
                        if (state.isBuffering) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }

            // Right: Play/Pause and Close buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Player",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Top progress highlight bar (accent line along top edge of capsule)
        if (state.durationMs > 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(state.progressFraction)
                    .height(2.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(alpha = 0.5f), accentColor)
                        )
                    )
            )
        }
    }
}

@Composable
private fun ExpandedIslandView(
    state: MediaPlaybackState,
    manager: MediaPlayerManager,
    accentColor: Color,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val cardShape = RoundedCornerShape(26.dp)
    val fileName = state.displayTitle ?: state.currentFile?.name ?: "Media"
    val isVideo = state.isVideo

    Box(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .drawBehind {
                // Frosted ambient radial backlight glow
                val glowRadius = size.maxDimension * 0.70f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            accentColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    )
                )
            }
            .clip(cardShape)
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.50f),
                        accentColor.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.14f)
                    )
                ),
                shape = cardShape
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20f) {
                        onCollapse()
                    }
                }
            }
    ) {
        // Frosted blur backdrop layers
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(20.dp)
                .background(accentColor.copy(alpha = 0.20f), cardShape)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A1D28).copy(alpha = 0.62f),
                            Color(0xFF0D0F16).copy(alpha = 0.74f)
                        )
                    ),
                    cardShape
                )
        )

        // Top specular reflection line
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.94f)
                .height(1.2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // TOP DRAG HANDLE (Invites swiping down to collapse)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )

        Spacer(modifier = Modifier.height(10.dp))

        // HEADER: Type badge, Speed toggle chip, External link, Collapse, Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isVideo) "VIDEO PLAYER" else "AUDIO PLAYER",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Playback speed pill chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .clickable { manager.cyclePlaybackSpeed() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${state.playbackSpeed}x",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (state.isBuffering) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Open in External Player (e.g. VLC)
                IconButton(
                    onClick = onOpenExternal,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = "Open in External Player",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Collapse into Island Pill
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize to Island",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Stop & Close
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Decoder issue. Tap launch icon above to stream in external player (e.g. VLC).",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // VIEWPORT: Embedded Video or High-Res Album Art / Audio Visualizer
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(0.5.dp, GlassHairline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (state.artworkUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(state.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                VideoTextureSurface(manager = manager)

                if (state.isBuffering) {
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else {
            // Audio Viewport: High-Res Album Artwork Card or Wave Visualizer
            if (state.artworkUrl != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.2.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.artworkUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Track Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    AnimatedAudioWaveVisualizer(isPlaying = state.isPlaying, color = accentColor)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        AnimatedAudioWaveVisualizer(isPlaying = state.isPlaying, color = accentColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // FILE INFO
        Text(
            text = fileName,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!state.displayArtist.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.displayArtist,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // SCRUBBER / SEEK SLIDER
        var isDraggingSlider by remember { mutableStateOf(false) }
        var dragPositionMs by remember { mutableFloatStateOf(0f) }

        val currentSliderPos = if (isDraggingSlider) {
            dragPositionMs
        } else {
            state.currentPositionMs.toFloat()
        }

        Slider(
            value = currentSliderPos,
            onValueChange = {
                isDraggingSlider = true
                dragPositionMs = it
            },
            onValueChangeFinished = {
                manager.seekTo(dragPositionMs.toLong())
                isDraggingSlider = false
            },
            valueRange = 0f..(state.durationMs.coerceAtLeast(1L).toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeMs(if (isDraggingSlider) dragPositionMs.toLong() else state.currentPositionMs),
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = formatTimeMs(state.durationMs),
                color = TextTertiary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // CONTROLS BAR: Loop, -10s, Play/Pause, +10s, Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Loop button
            IconButton(
                onClick = { manager.toggleLoop() },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (state.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Loop",
                    tint = if (state.isLooping) accentColor else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Seek backward 10s
            IconButton(
                onClick = { manager.seekRelative(-10000L) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "-10 seconds",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play / Pause prominent circular button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.85f))
                        )
                    )
                    .clickable { manager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Seek forward 10s
            IconButton(
                onClick = { manager.seekRelative(10000L) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "+10 seconds",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Speed cycle button
            IconButton(
                onClick = { manager.cyclePlaybackSpeed() },
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${state.playbackSpeed}x",
                        color = if (state.playbackSpeed != 1.0f) accentColor else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
}

/**
 * Native TextureView surface for rendering hardware-accelerated video frames.
 */
@Composable
private fun VideoTextureSurface(
    manager: MediaPlayerManager
) {
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        manager.attachSurface(Surface(st))
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        manager.detachSurface()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Animated 3-bar equalizer for compact island pill.
 */
@Composable
private fun AnimatedAudioEqualizer(
    isPlaying: Boolean,
    color: Color,
    barWidth: Dp = 3.dp,
    maxHeight: Float = 16f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_bars")

    val h1 by infiniteTransition.animateFloat(
        initialValue = maxHeight * 0.25f,
        targetValue = if (isPlaying) maxHeight else maxHeight * 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = maxHeight * 0.85f,
        targetValue = if (isPlaying) maxHeight * 0.3f else maxHeight * 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = maxHeight * 0.45f,
        targetValue = if (isPlaying) maxHeight * 1.1f else maxHeight * 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height((maxHeight + 2).dp)
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h1.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h2.dp)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(h3.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Visualizer animation bars for the audio card in expanded mode.
 */
@Composable
private fun AnimatedAudioWaveVisualizer(
    isPlaying: Boolean,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val heights = listOf(12, 24, 38, 18, 42, 28, 14, 32, 20, 36)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(48.dp)
    ) {
        heights.forEachIndexed { index, baseHeight ->
            val factor by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = if (isPlaying) 1f else 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(350 + (index * 45), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((baseHeight * factor).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.8f))
            )
        }
    }
}
