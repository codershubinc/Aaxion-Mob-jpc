package com.codershubinc.aaxion_mob.feature.drive

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.core.network.buildThumbUrl
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.FileVisualIcon
import com.codershubinc.aaxion_mob.ui.designsystem.resolveFileStyle
import com.codershubinc.aaxion_mob.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileItem,
    baseUrl: String,
    token: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showActionModal by remember { mutableStateOf(false) }

    val isVideo = file.isVideo
    val isAudio = file.isAudio
    val isImage = file.isImage

    val streamUrl = remember(baseUrl, file.rawPath, token) {
        file.buildStreamUrl(baseUrl, token)
    }
    val thumbUrl = remember(baseUrl, file.rawPath, token) {
        file.buildThumbUrl(baseUrl, token)
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(96)
            .build()
    }

    val fileStyle = resolveFileStyle(file)

    val dirManager = remember(context) { com.codershubinc.aaxion_mob.core.media.MediaDirectoryManager.getInstance(context) }
    val dirTypesMap by dirManager.directoryTypes.collectAsState()
    val designatedType = remember(dirTypesMap, file.rawPath) {
        if (file.isDir) dirManager.resolveDirectoryType(file.rawPath) else com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE
    }
    val explicitType = remember(dirTypesMap, file.rawPath) {
        if (file.isDir) dirManager.getExplicitType(file.rawPath) else com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE
    }

    // Only parse media metadata for video files or TV Series folders (never for movie directories or generic folders)
    val parsedMedia = remember(file.name, file.isDir, designatedType, explicitType) {
        when {
            file.isVideo -> com.codershubinc.aaxion_mob.core.media.MediaTitleParser.parse(
                file.name,
                isSeriesHint = designatedType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.SERIES
            )
            file.isDir && designatedType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.SERIES && explicitType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE ->
                com.codershubinc.aaxion_mob.core.media.MediaTitleParser.parse(file.name, isSeriesHint = true)
            else -> null
        }
    }

    val omdbDb = remember(context) { com.codershubinc.aaxion_mob.core.data.OmdbMetadataCacheDb.getInstance(context) }
    val omdbMap by omdbDb.cachedItems.collectAsState()
    val omdbItem = remember(omdbMap, file.rawPath, parsedMedia) {
        if (parsedMedia != null) omdbDb.getForMedia(parsedMedia, file.rawPath) else null
    }

    // A directory marked as Movies, any folder inside a Movies directory, or generic directories NEVER get posters.
    // Posters are ONLY for video files (movies/episodes) and TV series/season folders inside a Series directory.
    val isEligibleForPoster = when {
        file.isVideo -> true
        file.isDir -> {
            explicitType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE &&
            designatedType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.SERIES &&
            parsedMedia != null && parsedMedia.isSeries
        }
        else -> false
    }

    val hasOmdbPoster = isEligibleForPoster && omdbItem != null && omdbItem.hasPoster
    val seasonBadge = if (isEligibleForPoster) parsedMedia?.seasonEpisodeBadge else null

    AaxionCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AaxionSurfaceVariant,
        borderColor = if (isVideo) StreamerOrange.copy(alpha = 0.25f) else AaxionBorder,
        onClick = onClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (onLongClick != null) {
                onLongClick()
            } else {
                showActionModal = true
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon or Poster Thumbnail
            if (hasOmdbPoster) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C0C12))
                        .border(0.75.dp, GlassHairline, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val posterSource = remember(omdbItem?.posterUrl) {
                        com.codershubinc.aaxion_mob.core.data.PosterCacheManager.getInstance(context)
                            .resolvePosterSource(omdbItem?.posterUrl)
                    }
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(posterSource)
                            .crossfade(true)
                            .size(160)
                            .build(),
                        contentDescription = omdbItem!!.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (seasonBadge != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.88f))
                                .border(0.5.dp, StreamerPurple.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = seasonBadge,
                                color = Color(0xFFD4BFFF),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fileStyle.containerBrush),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImage) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        FileVisualIcon(
                            fileStyle = fileStyle,
                            iconSize = 22.dp
                        )
                    }

                    if (seasonBadge != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.88f))
                                .border(0.5.dp, StreamerPurple.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = seasonBadge,
                                color = StreamerPurple,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (file.isDir && designatedType != com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE) {
                        val isMovie = designatedType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.MOVIES
                        val badgeColor = if (isMovie) StreamerOrange else StreamerPurple
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(14.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(badgeColor)
                                .border(1.dp, Color.Black, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val displayTitle = if (hasOmdbPoster) {
                    parsedMedia?.getFormattedTitle(omdbItem?.title) ?: omdbItem!!.title
                } else {
                    file.displayName(40)
                }
                Text(
                    text = displayTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasOmdbPoster && omdbItem!!.rating.isNotBlank() && omdbItem.rating != "N/A") {
                        Text(
                            text = "★ ${omdbItem.rating}",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    if (seasonBadge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StreamerPurple.copy(alpha = 0.16f))
                                .border(0.5.dp, StreamerPurple.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = seasonBadge,
                                color = StreamerPurple,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "•", color = TextTertiary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    if (hasOmdbPoster && omdbItem!!.year.isNotBlank()) {
                        Text(
                            text = omdbItem.year,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "•", color = TextTertiary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    if (file.isDir) {
                        Text(text = "Directory", color = TextTertiary, fontSize = 11.sp)
                        if (designatedType != com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val isMovie = designatedType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.MOVIES
                            val badgeColor = if (isMovie) StreamerOrange else StreamerPurple
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.16f))
                                    .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isMovie) "MOVIES" else "SERIES",
                                    color = badgeColor,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    } else {
                        Text(text = IntentUtils.formatFileSize(file.size), color = TextTertiary, fontSize = 11.sp)
                    }

                    if (isVideo) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• VIDEO",
                            color = StreamerOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isAudio) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• AUDIO",
                            color = StreamerGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isAudio) {
                IconButton(
                    onClick = { MediaPlayerManager.getInstance(context).play(file, streamUrl, token = token, baseUrl = baseUrl) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play in Floating Island",
                        tint = StreamerGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isVideo) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play in VLC",
                        tint = StreamerOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                FileActionMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    file = file,
                    baseUrl = baseUrl,
                    token = token
                )
            }
        }
    }

    if (showActionModal) {
        FileActionModal(
            file = file,
            baseUrl = baseUrl,
            token = token,
            onDismiss = { showActionModal = false },
            onOpen = {
                showActionModal = false
                onClick()
            }
        )
    }
}

