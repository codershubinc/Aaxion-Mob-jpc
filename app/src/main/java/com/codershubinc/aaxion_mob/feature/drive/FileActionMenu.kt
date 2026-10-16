package com.codershubinc.aaxion_mob.feature.drive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
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
        if (file.isVideo || file.isAudio) {
            DropdownMenuItem(
                text = { Text("Stream Now", color = TextPrimary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (file.isVideo) StreamerOrange else StreamerGreen,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    onDismissRequest()
                    IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
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
