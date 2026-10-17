package com.codershubinc.aaxion_mob.feature.drive.viwerModals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.core.media.VideoProgress
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.ui.designsystem.GlassButton
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun VideoResumeModal(
    file: FileItem,
    progress: VideoProgress,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onPlayInternal: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val dialogShape = RoundedCornerShape(24.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(dialogShape)
                .background(Color(0xFF0C0C10).copy(alpha = 0.98f))
                .border(0.8.dp, GlassBrushes.specularBorder(StreamerOrange), dialogShape)
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StreamerOrange.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = StreamerOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceLow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Resume Video",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = file.displayName(40),
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StreamerOrange.copy(alpha = 0.14f))
                        .border(0.5.dp, StreamerOrange.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Last watched position: ${progress.formattedPosition}",
                        color = StreamerOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Action 1: Resume from last watched position
                GlassButton(
                    text = "Resume from ${progress.formattedPosition}",
                    onClick = {
                        onDismiss()
                        onResume()
                    },
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    accentColor = StreamerOrange,
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Start from beginning
                GlassButton(
                    text = "Play from Beginning",
                    onClick = {
                        onDismiss()
                        onStartOver()
                    },
                    icon = Icons.Default.Replay,
                    isPrimary = false,
                    accentColor = TextSecondary,
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onPlayInternal != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassButton(
                        text = "Play in Internal Player",
                        onClick = {
                            onDismiss()
                            onPlayInternal()
                        },
                        icon = Icons.Default.PlayArrow,
                        isPrimary = false,
                        accentColor = TextSecondary,
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
