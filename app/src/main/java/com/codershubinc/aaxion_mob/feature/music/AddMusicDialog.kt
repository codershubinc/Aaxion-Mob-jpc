package com.codershubinc.aaxion_mob.feature.music

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun AddMusicDialog(
    isAdding: Boolean,
    statusMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = {
            if (!isAdding) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isAdding,
            dismissOnClickOutside = !isAdding
        )
    ) {
        val dialogShape = RoundedCornerShape(20.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dialogShape)
                .background(AaxionCardBg)
                .border(1.dp, GlassHairline, dialogShape)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StreamerGreen.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = StreamerGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add to Music Library",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "YouTube Video or Playlist",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isAdding) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Paste a YouTube link below. The server will download high quality audio, embed official tags & album artwork, and add it to your library.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // URL Input Field with Paste & Clear actions
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    enabled = !isAdding,
                    placeholder = {
                        Text(
                            text = "https://www.youtube.com/watch?v=...",
                            color = TextTertiary,
                            fontSize = 12.5.sp
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlText.isNotEmpty() && !isAdding) {
                                IconButton(onClick = { urlText = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        urlText = clip.trim()
                                    }
                                },
                                enabled = !isAdding,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = StreamerGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StreamerGreen,
                        unfocusedBorderColor = GlassHairline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = StreamerGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null || isAdding) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StreamerGreen.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAdding) {
                            CircularProgressIndicator(
                                color = StreamerGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = statusMessage ?: "Downloading track from YouTube...",
                            color = StreamerGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isAdding) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            val trimmed = urlText.trim()
                            if (trimmed.isNotEmpty()) {
                                onAdd(trimmed)
                            }
                        },
                        enabled = urlText.isNotBlank() && !isAdding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamerGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = StreamerGreen.copy(alpha = 0.35f),
                            disabledContentColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdding) "Adding..." else "Download & Add",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
