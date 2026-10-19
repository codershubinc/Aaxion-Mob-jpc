package com.codershubinc.aaxion_mob.feature.drive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionToast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.codershubinc.aaxion_mob.core.media.DirectoryMediaType
import com.codershubinc.aaxion_mob.core.media.MediaDirectoryManager
import com.codershubinc.aaxion_mob.core.network.OmdbClient
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.buildDownloadUrl
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.feature.drive.viwerModals.*
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * File actions modal presenting actions in an inset grouped card list style.
 * Displays without an outer sheet container, directly floating over the dimmed backdrop.
 */
@Composable
fun FileActionModal(
    file: FileItem,
    baseUrl: String,
    token: String,
    onDismiss: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val currentAccent = LocalAccentColor.current

    val streamUrl = remember(baseUrl, file.rawPath, token) {
        file.buildStreamUrl(baseUrl, token)
    }
    val downloadUrl = remember(baseUrl, file.rawPath, token) {
        file.buildDownloadUrl(baseUrl, token)
    }

    val fileStyle = resolveFileStyle(file)

    val dirManager = remember(context) { MediaDirectoryManager.getInstance(context) }
    var currentExplicitType by remember(file.rawPath) {
        mutableStateOf(dirManager.getExplicitType(file.rawPath))
    }
    val resolvedDirType = remember(file.rawPath, currentExplicitType) {
        dirManager.resolveDirectoryType(file.rawPath)
    }

    val parsedMedia = remember(file.name, file.isDir, resolvedDirType, currentExplicitType) {
        when {
            file.isVideo -> com.codershubinc.aaxion_mob.core.media.MediaTitleParser.parse(
                file.name,
                isSeriesHint = resolvedDirType == DirectoryMediaType.SERIES
            )
            file.isDir && resolvedDirType == DirectoryMediaType.SERIES && currentExplicitType == DirectoryMediaType.NONE ->
                com.codershubinc.aaxion_mob.core.media.MediaTitleParser.parse(file.name, isSeriesHint = true)
            else -> null
        }
    }

    val omdbDb = remember(context) { com.codershubinc.aaxion_mob.core.data.OmdbMetadataCacheDb.getInstance(context) }
    val omdbMap by omdbDb.cachedItems.collectAsState()
    var cachedOmdb by remember(omdbMap, file.rawPath, parsedMedia) {
        mutableStateOf(if (parsedMedia != null) omdbDb.getForMedia(parsedMedia, file.rawPath) else null)
    }
    var isFetchingOmdb by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val isEligibleForPoster = when {
        file.isVideo -> true
        file.isDir -> {
            currentExplicitType == DirectoryMediaType.NONE &&
            resolvedDirType == DirectoryMediaType.SERIES &&
            parsedMedia != null && parsedMedia.isSeries
        }
        else -> false
    }

    val hasPoster = isEligibleForPoster && cachedOmdb != null && cachedOmdb!!.hasPoster

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        SyncDialogWindow(clearDimBehind = true)

        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 420.dp else 400.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside the card
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Inside List Style: Grouped Card containing Header + Actions
                InsetGroupedGlassCard(
                    accentHighlight = currentAccent
                ) {
                    // Header item
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                    ) {
                        val seasonBadge = parsedMedia?.seasonEpisodeBadge
                        if (hasPoster) {
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0C0C12))
                                    .border(0.8.dp, GlassHairline, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                coil.compose.AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(cachedOmdb!!.posterUrl)
                                        .crossfade(true)
                                        .size(180)
                                        .build(),
                                    contentDescription = cachedOmdb!!.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                        } else if (file.isDir && resolvedDirType != DirectoryMediaType.NONE) {
                            val isMovie = resolvedDirType == DirectoryMediaType.MOVIES
                            val badgeColor = if (isMovie) StreamerOrange else StreamerPurple
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .border(0.8.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(fileStyle.containerBrush)
                                    .border(0.8.dp, fileStyle.tint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                FileVisualIcon(
                                    fileStyle = fileStyle,
                                    iconSize = 26.dp
                                )

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
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val headerTitle = if (hasPoster) {
                                parsedMedia?.getFormattedTitle(cachedOmdb?.title) ?: cachedOmdb!!.title
                            } else {
                                file.displayName(36)
                            }
                            Text(
                                text = headerTitle,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val subtitleText = when {
                                    file.isDir && parsedMedia?.seasonNumber != null ->
                                        "Season ${parsedMedia.seasonNumber} • Directory"
                                    file.isDir -> "Directory"
                                    parsedMedia != null && parsedMedia.seasonBadgeLong != null ->
                                        "${parsedMedia.seasonBadgeLong} • ${IntentUtils.formatFileSize(file.size)}"
                                    cachedOmdb != null && cachedOmdb!!.year.isNotBlank() ->
                                        "${cachedOmdb!!.year} • ${IntentUtils.formatFileSize(file.size)}"
                                    else -> IntentUtils.formatFileSize(file.size)
                                }
                                Text(
                                    text = subtitleText,
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                                if (file.isDir && resolvedDirType != DirectoryMediaType.NONE) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val isMovie = resolvedDirType == DirectoryMediaType.MOVIES
                                    val badgeColor = if (isMovie) StreamerOrange else StreamerPurple
                                    Text(
                                        text = if (isMovie) "• MARKED AS MOVIES" else "• MARKED AS TV SERIES",
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (file.isVideo || file.isAudio) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• " + (if (file.isVideo) "VIDEO" else "AUDIO"),
                                        color = if (file.isVideo) StreamerOrange else StreamerGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Compact OMDB Movie Synopsis Card if available
                    if (hasPoster && cachedOmdb != null && (cachedOmdb!!.plot.isNotBlank() || cachedOmdb!!.rating.isNotBlank())) {
                        InsetGlassDivider(startIndent = 14.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OMDB SYNOPSIS",
                                    color = StreamerOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                if (cachedOmdb!!.rating.isNotBlank() && cachedOmdb!!.rating != "N/A") {
                                    Text(
                                        text = "★ ${cachedOmdb!!.rating}/10 IMDb",
                                        color = Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (cachedOmdb!!.plot.isNotBlank() && cachedOmdb!!.plot != "N/A") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cachedOmdb!!.plot,
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (cachedOmdb!!.genre.isNotBlank() && cachedOmdb!!.genre != "N/A") {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Genre: ${cachedOmdb!!.genre}",
                                    color = TextTertiary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    InsetGlassDivider(startIndent = 14.dp)

                    // Action: Open / Navigate
                    val isViewable = canOpenFileInViewer(file)
                    ModalActionItem(
                        icon = if (file.isDir) Icons.Default.FolderOpen else if (isViewable) Icons.Default.Visibility else Icons.AutoMirrored.Filled.Launch,
                        title = if (file.isDir) "Open Directory" else if (isViewable) "Preview Document" else "Open / View",
                        subtitle = if (file.isDir) "Browse contents of this folder" else if (isViewable) "Open directly inside app" else "Open with default handler",
                        accentColor = currentAccent,
                        onClick = onOpen
                    )

                    if (file.isDir) {
                        var isMarkSubmenuOpen by remember { mutableStateOf(false) }

                        val currentTypeLabel = when (currentExplicitType) {
                            DirectoryMediaType.SERIES -> "TV Series"
                            DirectoryMediaType.MOVIES -> "Movies"
                            DirectoryMediaType.NONE -> "None / Standard"
                        }
                        val currentTypeAccent = when (currentExplicitType) {
                            DirectoryMediaType.SERIES -> StreamerPurple
                            DirectoryMediaType.MOVIES -> StreamerOrange
                            DirectoryMediaType.NONE -> TextSecondary
                        }
                        val currentTypeIcon = when (currentExplicitType) {
                            DirectoryMediaType.SERIES -> Icons.Default.LiveTv
                            DirectoryMediaType.MOVIES -> Icons.Default.Movie
                            DirectoryMediaType.NONE -> Icons.Default.FolderOpen
                        }

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = currentTypeIcon,
                            title = "Mark Current Folder as",
                            subtitle = "Currently: $currentTypeLabel • Tap to choose",
                            accentColor = currentTypeAccent,
                            onClick = {
                                isMarkSubmenuOpen = !isMarkSubmenuOpen
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = if (isMarkSubmenuOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isMarkSubmenuOpen) "Collapse Submenu" else "Expand Submenu",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        AnimatedVisibility(
                            visible = isMarkSubmenuOpen,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.02f))
                            ) {
                                InsetGlassDivider(startIndent = 28.dp)
                                ModalSubmenuChoiceItem(
                                    icon = Icons.Default.LiveTv,
                                    title = "TV Series",
                                    subtitle = "Auto-fetch posters for series folders & seasons",
                                    isSelected = currentExplicitType == DirectoryMediaType.SERIES,
                                    accentColor = StreamerPurple,
                                    onClick = {
                                        dirManager.setDirectoryType(file.rawPath, DirectoryMediaType.SERIES)
                                        currentExplicitType = DirectoryMediaType.SERIES
                                        AaxionToast.show(context, "Marked as TV Series")
                                    }
                                )

                                InsetGlassDivider(startIndent = 28.dp)
                                ModalSubmenuChoiceItem(
                                    icon = Icons.Default.Movie,
                                    title = "Movies",
                                    subtitle = "Auto-fetch posters & ratings for movie files",
                                    isSelected = currentExplicitType == DirectoryMediaType.MOVIES,
                                    accentColor = StreamerOrange,
                                    onClick = {
                                        dirManager.setDirectoryType(file.rawPath, DirectoryMediaType.MOVIES)
                                        currentExplicitType = DirectoryMediaType.MOVIES
                                        AaxionToast.show(context, "Marked as Movies")
                                    }
                                )

                                InsetGlassDivider(startIndent = 28.dp)
                                ModalSubmenuChoiceItem(
                                    icon = Icons.Default.FolderOpen,
                                    title = "None (Standard Folder)",
                                    subtitle = "Revert to standard folder browsing",
                                    isSelected = currentExplicitType == DirectoryMediaType.NONE,
                                    accentColor = TextPrimary,
                                    onClick = {
                                        dirManager.setDirectoryType(file.rawPath, DirectoryMediaType.NONE)
                                        currentExplicitType = DirectoryMediaType.NONE
                                        AaxionToast.show(context, "Media designation cleared")
                                    }
                                )
                            }
                        }
                    }

                    if (file.isAudio) {
                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.Default.PlayArrow,
                            title = "Play in Internal Player",
                            subtitle = "Continuous in-app playback with capsule & player card",
                            accentColor = StreamerGreen,
                            onClick = {
                                onDismiss()
                                MediaPlayerManager.getInstance(context).play(file, streamUrl, token = token, baseUrl = baseUrl)
                            }
                        )

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.AutoMirrored.Filled.Launch,
                            title = "Stream Audio Externally",
                            subtitle = "Open stream via external player",
                            accentColor = TextSecondary,
                            onClick = {
                                onDismiss()
                                IntentUtils.launchStreamIntent(context, streamUrl, isVideo = false, title = file.name)
                            }
                        )
                    } else if (file.isVideo) {
                        val vlcManager = remember(context) { VlcPlayerManager.getInstance(context) }
                        val videoProgress = remember(file.rawPath) { vlcManager.getProgress(file.rawPath) }
                        val hasResume = videoProgress != null && videoProgress.positionMs >= 5000L

                        val vlcTitle = parsedMedia?.getFormattedTitle(cachedOmdb?.title)
                            ?: cachedOmdb?.title
                            ?: file.displayName(40)
                        val vlcPoster = cachedOmdb?.posterUrl

                        if (hasResume) {
                            InsetGlassDivider(startIndent = 56.dp)
                            ModalActionItem(
                                icon = Icons.Default.PlayArrow,
                                title = "Resume Video in VLC (${videoProgress.formattedPosition})",
                                subtitle = "Continue watching from saved position",
                                accentColor = StreamerOrange,
                                onClick = {
                                    onDismiss()
                                    vlcManager.launchVlc(context, file, streamUrl, resume = true, token = token, customTitle = vlcTitle, posterUrl = vlcPoster)
                                }
                            )

                            InsetGlassDivider(startIndent = 56.dp)
                            ModalActionItem(
                                icon = Icons.Default.Replay,
                                title = "Start from Beginning",
                                subtitle = "Play video from 00:00 in VLC",
                                accentColor = TextSecondary,
                                onClick = {
                                    onDismiss()
                                    vlcManager.launchVlc(context, file, streamUrl, resume = false, token = token, customTitle = vlcTitle, posterUrl = vlcPoster)
                                }
                            )
                        } else {
                            InsetGlassDivider(startIndent = 56.dp)
                            ModalActionItem(
                                icon = Icons.Default.PlayArrow,
                                title = "Play in VLC",
                                subtitle = "Stream video directly in VLC player",
                                accentColor = StreamerOrange,
                                onClick = {
                                    onDismiss()
                                    vlcManager.launchVlc(context, file, streamUrl, resume = false, token = token, customTitle = vlcTitle, posterUrl = vlcPoster)
                                }
                            )
                        }

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.Default.Info,
                            title = "Video Stats in Floating Island",
                            subtitle = "Inspect resolution, bitrate, and SQLite DB cache metrics",
                            accentColor = StreamerOrange,
                            onClick = {
                                onDismiss()
                                vlcManager.inspectVideo(file, streamUrl, token = token)
                            }
                        )

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.Default.PlayArrow,
                            title = "Play in Internal Player",
                            subtitle = "Continuous in-app playback with floating island",
                            accentColor = StreamerOrange,
                            onClick = {
                                onDismiss()
                                MediaPlayerManager.getInstance(context).play(file, streamUrl, token = token, baseUrl = baseUrl)
                            }
                        )

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.AutoMirrored.Filled.Launch,
                            title = "Stream in External Player",
                            subtitle = "Open stream via external player chooser",
                            accentColor = TextSecondary,
                            onClick = {
                                onDismiss()
                                IntentUtils.launchStreamIntent(context, streamUrl, isVideo = true, title = vlcTitle, posterUrl = vlcPoster)
                            }
                        )

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.Default.Sync,
                            title = if (cachedOmdb != null) "Refresh OMDB Metadata" else "Fetch OMDB Movie Info",
                            subtitle = if (isFetchingOmdb) "Querying OMDB API..." else (if (cachedOmdb != null) "Refetch poster, IMDb rating & synopsis" else "Query OMDB using clean title & year"),
                            accentColor = StreamerOrange,
                            onClick = {
                                if (!isFetchingOmdb) {
                                    isFetchingOmdb = true
                                    coroutineScope.launch {
                                        val res = OmdbClient.fetchMovieMetadata(
                                            context = context,
                                            fileName = file.name,
                                            rawPath = file.rawPath,
                                            forceRefresh = true
                                        )
                                        isFetchingOmdb = false
                                        if (res != null) {
                                            cachedOmdb = res
                                            AaxionToast.show(context, "OMDB: ${res.title} (${res.year})")
                                        } else {
                                            AaxionToast.show(context, "No OMDB match found")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Download Action (For files)
                    if (!file.isDir) {
                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.Default.Download,
                            title = "Download File",
                            subtitle = "Save a copy directly to your device",
                            accentColor = currentAccent,
                            onClick = {
                                onDismiss()
                                IntentUtils.launchDownloadIntent(context, downloadUrl, fileName = file.name, token = token)
                            }
                        )

                        InsetGlassDivider(startIndent = 56.dp)
                        // Copy Stream Link
                        ModalActionItem(
                            icon = Icons.Default.ContentCopy,
                            title = "Copy Stream URL",
                            subtitle = "Copy active bearer stream URL to clipboard",
                            accentColor = TextSecondary,
                            onClick = {
                                onDismiss()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Stream URL", streamUrl)
                                clipboard.setPrimaryClip(clip)
                                AaxionToast.show(context, "Stream link copied to clipboard")
                            }
                        )
                    }

                    InsetGlassDivider(startIndent = 56.dp)
                    // Copy Full Path
                    ModalActionItem(
                        icon = Icons.Default.Folder,
                        title = "Copy Server Path",
                        subtitle = file.rawPath,
                        accentColor = TextTertiary,
                        onClick = {
                            onDismiss()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("File Path", file.rawPath)
                            clipboard.setPrimaryClip(clip)
                            AaxionToast.show(context, "File path copied to clipboard")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassButton(
                    text = "Cancel",
                    isPrimary = false,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ModalActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GlassSurfaceLow)
                .border(0.8.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
private fun ModalSubmenuChoiceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 28.dp, end = 14.dp, top = 9.dp, bottom = 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) accentColor.copy(alpha = 0.16f) else GlassSurfaceLow)
                .border(0.8.dp, if (isSelected) accentColor.copy(alpha = 0.6f) else GlassHairline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
