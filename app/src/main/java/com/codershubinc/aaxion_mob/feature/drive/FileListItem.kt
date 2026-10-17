package com.codershubinc.aaxion_mob.feature.drive

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
            // Icon or Thumbnail
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
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName(40),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (file.isDir) {
                        Text(text = "Directory", color = TextTertiary, fontSize = 11.sp)
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

            // Quick Stream Action (Floating Island)
            if (isVideo || isAudio) {
                IconButton(
                    onClick = { MediaPlayerManager.getInstance(context).play(file, streamUrl, token = token, baseUrl = baseUrl) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play in Floating Island",
                        tint = if (isVideo) StreamerOrange else StreamerGreen,
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

