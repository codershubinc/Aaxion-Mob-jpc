package com.codershubinc.aaxion_mob.ui.components.files

import android.content.Intent
import androidx.compose.foundation.background
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
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
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.ui.components.AaxionCard
import com.codershubinc.aaxion_mob.ui.components.FileActionModal
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.IntentUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showActionModal by remember { mutableStateOf(false) }

    val isVideo = file.isVideo
    val isAudio = file.isAudio
    val isImage = file.isImage

    val encodedPath = remember(file.rawPath) {
        try {
            URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            file.rawPath
        }
    }

    val streamUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/files/stream?path=$encodedPath&tkn=$token"
    }
    val downloadUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/files/download?path=$encodedPath&tkn=$token"
    }
    val thumbUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/images/thumb?path=$encodedPath&tkn=$token"
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(96)
            .build()
    }

    val currentAccent = com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor.current
    val currentIconPack = com.codershubinc.aaxion_mob.ui.theme.LocalIconPack.current

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
                    .background(
                        when {
                            file.isDir -> currentAccent.copy(alpha = 0.12f)
                            isVideo -> StreamerOrange.copy(alpha = 0.12f)
                            isAudio -> StreamerGreen.copy(alpha = 0.12f)
                            isImage -> StreamerPink.copy(alpha = 0.12f)
                            else -> currentAccent.copy(alpha = 0.12f)
                        }
                    ),
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
                    Icon(
                        imageVector = when {
                            file.isDir -> when (currentIconPack) {
                                com.codershubinc.aaxion_mob.models.IconPackTheme.MINIMAL_ROUNDED -> Icons.Default.Folder
                                com.codershubinc.aaxion_mob.models.IconPackTheme.SYSTEM_FILLED -> Icons.Default.FolderShared
                                com.codershubinc.aaxion_mob.models.IconPackTheme.OUTLINE -> Icons.Default.FolderOpen
                                com.codershubinc.aaxion_mob.models.IconPackTheme.CYBER_NEON -> Icons.Default.FolderSpecial
                            }
                            isVideo -> Icons.Default.PlayCircleOutline
                            isAudio -> Icons.Default.MusicNote
                            file.isDoc -> Icons.Default.Description
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = when {
                            file.isDir -> currentAccent
                            isVideo -> StreamerOrange
                            isAudio -> StreamerGreen
                            isImage -> StreamerPink
                            else -> currentAccent
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
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

            // Quick Stream / Download Action
            if (isVideo || isAudio) {
                IconButton(
                    onClick = { IntentUtils.launchStreamIntent(context, streamUrl, isVideo, title = file.name) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Stream",
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

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(AaxionSurfaceHigh)
                ) {
                    if (isVideo || isAudio) {
                        DropdownMenuItem(
                            text = { Text("Stream Now", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isVideo) StreamerOrange else StreamerGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                IntentUtils.launchStreamIntent(context, streamUrl, isVideo, title = file.name)
                            }
                        )
                    }

                    if (!file.isDir) {
                        DropdownMenuItem(
                            text = { Text("Download", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = AaxionPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                IntentUtils.launchDownloadIntent(context, downloadUrl, fileName = file.name, token = token)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Copy Stream Link", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Stream URL", streamUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Stream link copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
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

