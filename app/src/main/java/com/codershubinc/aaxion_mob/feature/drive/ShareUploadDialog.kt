package com.codershubinc.aaxion_mob.feature.drive

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.core.upload.UploadManager
import com.codershubinc.aaxion_mob.core.upload.UploadTask
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionToast
import com.codershubinc.aaxion_mob.ui.theme.GlassBrushes
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * AMOLED Glass dialog for uploading files shared from other applications to Aaxion server.
 */
@Composable
fun ShareUploadDialog(
    sharedUris: List<Uri>,
    serverUri: String,
    token: String,
    initialTargetDir: String = "/",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    var targetDir by remember { mutableStateOf(initialTargetDir.ifBlank { "/" }) }
    val uploadManager = remember { UploadManager.getInstance(context) }
    val isLocal = remember(serverUri) { uploadManager.isLocalServerUri(serverUri) }

    val fileItems = remember(sharedUris) {
        sharedUris.map { uri ->
            var name = uri.lastPathSegment ?: "file"
            var size = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) cursor.getString(nameIdx)?.let { name = it }
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
            } catch (_: Exception) {}
            Pair(name, size)
        }
    }

    val dialogShape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.95f)
                .clip(dialogShape)
                .background(Color(0xFF090D14))
                .border(0.8.dp, GlassBrushes.specularBorder(accentColor), dialogShape)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upload Shared ${if (sharedUris.size > 1) "${sharedUris.size} Files" else "File"}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val modeText = if (isLocal) "Direct Local Upload" else "Tunnel Chunked Upload"
                            val modeColor = if (isLocal) StreamerGreen else StreamerOrange
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(modeColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = modeText,
                                    color = modeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target directory input
                Text(
                    text = "Destination Directory",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetDir,
                    onValueChange = { targetDir = it },
                    singleLine = true,
                    placeholder = { Text("/home/user/...", color = TextTertiary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = GlassHairline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Files preview list
                Text(
                    text = "Items (${sharedUris.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.heightIn(max = 160.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(fileItems) { (name, size) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(0.5.dp, GlassHairline, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = UploadTask.formatBytes(size),
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (serverUri.isNotBlank()) {
                                uploadManager.enqueueUploads(
                                    context = context,
                                    serverUri = serverUri,
                                    token = token,
                                    targetDir = targetDir.ifBlank { "/" },
                                    uris = sharedUris
                                )
                                AaxionToast.show(context, "Upload queued in background")
                            } else {
                                AaxionToast.show(context, "No server connected")
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
