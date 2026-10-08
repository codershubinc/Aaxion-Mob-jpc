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

    val encodedPath = remember(file.rawPath) {
        try {
            URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            file.rawPath
        }
    }

    val thumbUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/images/thumb?path=$encodedPath&tkn=$token"
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(128)
            .build()
    }

    val currentAccent = com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor.current
    val currentIconPack = com.codershubinc.aaxion_mob.ui.theme.LocalIconPack.current

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
        // Just the Icon / Thumbnail without background tile or border
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (file.isImage) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
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
                        file.isVideo -> Icons.Default.PlayCircleOutline
                        file.isAudio -> Icons.Default.MusicNote
                        file.isDoc -> Icons.Default.Description
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = when {
                        file.isDir -> currentAccent
                        file.isVideo -> StreamerOrange
                        file.isAudio -> StreamerGreen
                        file.isDoc -> currentAccent
                        else -> TextSecondary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Just the name below
        Text(
            text = file.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
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
