package com.codershubinc.aaxion_mob.feature.drive

import android.content.Intent
import androidx.compose.foundation.background
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.buildThumbUrl
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.FileVisualIcon
import com.codershubinc.aaxion_mob.ui.designsystem.resolveFileStyle
import com.codershubinc.aaxion_mob.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileItem,
    baseUrl: String,
    token: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showActionModal by remember { mutableStateOf(false) }

    val thumbUrl = remember(baseUrl, file.rawPath, token) {
        file.buildThumbUrl(baseUrl, token)
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(128)
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        showActionModal = true
                    }
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        if (hasOmdbPoster) {
            // High-resolution OMDB 2:3 Movie / Series Poster Card
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(98.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E0E14))
                    .border(0.75.dp, GlassHairline, RoundedCornerShape(12.dp)),
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
                        .size(240)
                        .build(),
                    contentDescription = omdbItem!!.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Rating Badge (Top End)
                if (omdbItem.rating.isNotBlank() && omdbItem.rating != "N/A") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "★ ${omdbItem.rating}",
                            color = Color(0xFFFFD700),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Season / Episode Badge on Bottom Right of Poster
                if (seasonBadge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.88f))
                            .border(0.6.dp, StreamerPurple.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = seasonBadge,
                            color = Color(0xFFD4BFFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        } else {
            // Translucent obsidian glass tile with specular border and soft radial glow
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurfaceLow)
                    .border(1.dp, AaxionBorder, RoundedCornerShape(12.dp))
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    fileStyle.tint.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 1.4f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (file.isImage) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(11.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FileVisualIcon(
                        fileStyle = fileStyle,
                        iconSize = 36.dp
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
                            .padding(2.dp)
                            .size(18.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(badgeColor)
                            .border(1.2.dp, Color.Black, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title and season/episode info
        val displayTitle = if (hasOmdbPoster) {
            parsedMedia?.getCompactGridTitle(omdbItem?.title) ?: omdbItem!!.title
        } else {
            file.displayName(30)
        }
        Text(
            text = displayTitle,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (hasOmdbPoster) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        // Subtitle: Series title for episodes, year for movies
        val subtitleText = when {
            parsedMedia != null && parsedMedia.isSeries && parsedMedia.episodeNumber != null -> {
                omdbItem?.title?.takeIf { it.isNotBlank() } ?: parsedMedia.cleanTitle
            }
            hasOmdbPoster && omdbItem!!.year.isNotBlank() -> omdbItem.year
            parsedMedia != null && parsedMedia.year != null -> parsedMedia.year
            else -> null
        }

        if (subtitleText != null) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitleText,
                color = TextTertiary,
                fontSize = 9.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (file.isDir && designatedType != com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.NONE) {
            val isMovie = designatedType == com.codershubinc.aaxion_mob.core.media.DirectoryMediaType.MOVIES
            val badgeColor = if (isMovie) StreamerOrange else StreamerPurple
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.18f))
                    .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (isMovie) "MOVIES" else "SERIES",
                    color = badgeColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
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
