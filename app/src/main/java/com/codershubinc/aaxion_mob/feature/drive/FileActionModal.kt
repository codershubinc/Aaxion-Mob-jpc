package com.codershubinc.aaxion_mob.feature.drive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
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
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.displayName(36),
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

                    // Stream Actions (Floating Island & External Player)
                    if (file.isVideo || file.isAudio) {
                        InsetGlassDivider(startIndent = 56.dp)
                        val streamColor = if (file.isVideo) StreamerOrange else StreamerGreen
                        ModalActionItem(
                            icon = Icons.Default.PlayArrow,
                            title = if (file.isVideo) "Play in Floating Island" else "Play in Floating Island",
                            subtitle = "Continuous in-app playback with capsule & player card",
                            accentColor = streamColor,
                            onClick = {
                                onDismiss()
                                MediaPlayerManager.getInstance(context).play(file, streamUrl, token = token, baseUrl = baseUrl)
                            }
                        )

                        InsetGlassDivider(startIndent = 56.dp)
                        ModalActionItem(
                            icon = Icons.AutoMirrored.Filled.Launch,
                            title = if (file.isVideo) "Stream in External Player" else "Stream Audio Externally",
                            subtitle = "Open stream via external player (e.g. VLC)",
                            accentColor = TextSecondary,
                            onClick = {
                                onDismiss()
                                IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
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
                                Toast.makeText(context, "Stream link copied to clipboard", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, "File path copied to clipboard", Toast.LENGTH_SHORT).show()
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
    onClick: () -> Unit
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
    }
}
