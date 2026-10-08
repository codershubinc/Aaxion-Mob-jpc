package com.codershubinc.aaxion_mob.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.IntentUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
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
    val currentIconPack = LocalIconPack.current

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF262626))
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header: Icon + Name + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                file.isDir -> currentAccent.copy(alpha = 0.15f)
                                file.isVideo -> StreamerOrange.copy(alpha = 0.15f)
                                file.isAudio -> StreamerGreen.copy(alpha = 0.15f)
                                file.isImage -> StreamerPink.copy(alpha = 0.15f)
                                else -> currentAccent.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
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
                        modifier = Modifier.size(26.dp),
                        tint = when {
                            file.isDir -> currentAccent
                            file.isVideo -> StreamerOrange
                            file.isAudio -> StreamerGreen
                            file.isDoc -> currentAccent
                            else -> TextSecondary
                        }
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (file.isDir) "Directory" else IntentUtils.formatFileSize(file.size),
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
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

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color(0xFF1F1F1F), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action: Open / Navigate
            ModalActionItem(
                icon = if (file.isDir) Icons.Default.FolderOpen else Icons.AutoMirrored.Filled.Launch,
                title = if (file.isDir) "Open Directory" else "Open / View",
                subtitle = if (file.isDir) "Browse contents of this folder" else "Open with default handler",
                accentColor = currentAccent,
                onClick = onOpen
            )

            // Stream Action (For Video/Audio)
            if (file.isVideo || file.isAudio) {
                val streamColor = if (file.isVideo) StreamerOrange else StreamerGreen
                ModalActionItem(
                    icon = Icons.Default.PlayArrow,
                    title = if (file.isVideo) "Stream Video (VLC)" else "Play Audio Stream (VLC)",
                    subtitle = "Stream instantly via VLC or external media player",
                    accentColor = streamColor,
                    onClick = {
                        onDismiss()
                        IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
                    }
                )
            }

            // Download Action (For files)
            if (!file.isDir) {
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
                        Toast.makeText(context, "Stream link copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }

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
                    Toast.makeText(context, "File path copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ModalActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.12f)),
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
    }
}
