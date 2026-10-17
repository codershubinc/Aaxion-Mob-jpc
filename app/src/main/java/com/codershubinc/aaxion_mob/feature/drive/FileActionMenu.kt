package com.codershubinc.aaxion_mob.feature.drive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.DropdownMenuItem
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.buildDownloadUrl
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.GlassContextMenu
import com.codershubinc.aaxion_mob.ui.designsystem.GlassMenuDivider
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * Spatial glass floating action menu for file items.
 */
@Composable
fun FileActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    file: FileItem,
    baseUrl: String,
    token: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    val streamUrl = remember(baseUrl, file.rawPath, token) {
        file.buildStreamUrl(baseUrl, token)
    }
    val downloadUrl = remember(baseUrl, file.rawPath, token) {
        file.buildDownloadUrl(baseUrl, token)
    }

    GlassContextMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        accentHighlight = accentColor,
        modifier = modifier
    ) {
        if (file.isAudio) {
            DropdownMenuItem(
                text = { Text("Play in Island", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = StreamerGreen,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    MediaPlayerManager.getInstance(context).play(file, streamUrl, token = token, baseUrl = baseUrl)
                }
            )

            DropdownMenuItem(
                text = { Text("Open in External Player", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    IntentUtils.launchStreamIntent(context, streamUrl, isVideo = false, title = file.name)
                }
            )

            if (!file.isDir) {
                GlassMenuDivider()
            }
        } else if (file.isVideo) {
            val vlcManager = remember(context) { VlcPlayerManager.getInstance(context) }
            val videoProgress = remember(file.rawPath) { vlcManager.getProgress(file.rawPath) }
            val hasResume = videoProgress != null && videoProgress.positionMs >= 5000L

            if (hasResume) {
                DropdownMenuItem(
                    text = { Text("Resume in VLC (${videoProgress.formattedPosition})", color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = StreamerOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        onDismissRequest()
                        vlcManager.launchVlc(context, file, streamUrl, resume = true)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Play from Beginning", color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        onDismissRequest()
                        vlcManager.launchVlc(context, file, streamUrl, resume = false)
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Play in VLC", color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = StreamerOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        onDismissRequest()
                        vlcManager.launchVlc(context, file, streamUrl, resume = false)
                    }
                )
            }

            DropdownMenuItem(
                text = { Text("Open in External Player", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    IntentUtils.launchStreamIntent(context, streamUrl, isVideo = true, title = file.name)
                }
            )

            if (!file.isDir) {
                GlassMenuDivider()
            }
        }

        if (!file.isDir) {
            DropdownMenuItem(
                text = { Text("Download", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    IntentUtils.launchDownloadIntent(context, downloadUrl, fileName = file.name, token = token)
                }
            )

            GlassMenuDivider()

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
                    onDismissRequest()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Stream URL", streamUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Stream link copied", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
