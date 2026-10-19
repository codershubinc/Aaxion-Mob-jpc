package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.codershubinc.aaxion_mob.core.upload.UploadManager
import com.codershubinc.aaxion_mob.core.upload.UploadStatus
import com.codershubinc.aaxion_mob.core.upload.UploadTask
import com.codershubinc.aaxion_mob.ui.theme.GlassBrushes
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * Mobile floating island upload capsule and expandable bottom sheet for real-time upload tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileUploadStatus(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val uploadManager = remember { UploadManager.getInstance(context) }
    val tasks by uploadManager.tasks.collectAsState()
    val hasActive by uploadManager.hasActiveUploads.collectAsState()
    val activeSpeed by uploadManager.activeSpeed.collectAsState()

    var showModalSheet by remember { mutableStateOf(false) }

    if (tasks.isEmpty()) return

    val activeCount = tasks.count { it.status == UploadStatus.UPLOADING || it.status == UploadStatus.QUEUED }
    val completedCount = tasks.count { it.status == UploadStatus.SUCCESS }
    val failedCount = tasks.count { it.status == UploadStatus.FAILED }
    val totalCount = tasks.size

    val overallProgress = remember(tasks) {
        if (tasks.isEmpty()) 0f
        else tasks.map { it.progress }.average().toFloat()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_mobile")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlphaMobile"
    )

    val capsuleShape = RoundedCornerShape(50)

    // Floating Capsule Pill
    AnimatedVisibility(
        visible = tasks.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(capsuleShape)
                .background(GlassBrushes.glassSurface())
                .border(0.8.dp, GlassBrushes.specularBorder(accentColor), capsuleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showModalSheet = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (hasActive) pulseAlpha * 0.3f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasActive) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                            contentDescription = "Upload status",
                            tint = if (hasActive) accentColor else StreamerGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        val title = if (hasActive) {
                            "Uploading $activeCount of $totalCount files"
                        } else {
                            "Upload complete ($completedCount done)"
                        }
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = if (hasActive) {
                            "${UploadTask.formatBytes(activeSpeed)}/s • ${(overallProgress * 100).toInt()}%"
                        } else {
                            if (failedCount > 0) "$failedCount failed • Tap to view" else "Tap to view details"
                        }
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasActive) {
                        CircularProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier.size(20.dp),
                            color = accentColor,
                            trackColor = GlassHairline,
                            strokeWidth = 2.5.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand uploads sheet",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Expandable Detailed Modal Bottom Sheet
    if (showModalSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModalSheet = false },
            containerColor = Color(0xFF090D14),
            scrimColor = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GlassHairline)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "File Uploads",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val summaryText = if (hasActive) {
                            "$activeCount active • ${UploadTask.formatBytes(activeSpeed)}/s"
                        } else {
                            "$completedCount completed" + if (failedCount > 0) ", $failedCount failed" else ""
                        }
                        Text(
                            text = summaryText,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Row {
                        if (hasActive) {
                            TextButton(onClick = { uploadManager.cancelAll() }) {
                                Text("Cancel All", color = StreamerOrange, fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = {
                                uploadManager.clearFinished()
                                showModalSheet = false
                            }) {
                                Text("Clear", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = GlassHairline, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Tasks List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        MobileUploadTaskItem(
                            task = task,
                            accentColor = accentColor,
                            onCancel = { uploadManager.cancelUpload(task.id) },
                            onRetry = { uploadManager.retryUpload(context, task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileUploadTaskItem(
    task: UploadTask,
    accentColor: Color,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val statusColor = when (task.status) {
        UploadStatus.QUEUED -> TextTertiary
        UploadStatus.UPLOADING -> accentColor
        UploadStatus.SUCCESS -> StreamerGreen
        UploadStatus.FAILED -> StreamerOrange
        UploadStatus.CANCELLED -> TextMuted
    }

    val itemShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, GlassHairline, itemShape)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = when (task.status) {
                    UploadStatus.QUEUED -> "Queued • ${task.formattedSize}"
                    UploadStatus.UPLOADING -> "${task.formattedUploaded} / ${task.formattedSize} • ${task.formattedSpeed}"
                    UploadStatus.SUCCESS -> "Uploaded • ${task.formattedSize}"
                    UploadStatus.FAILED -> task.errorMessage ?: "Upload failed"
                    UploadStatus.CANCELLED -> "Cancelled"
                }
                Text(
                    text = subtitle,
                    color = statusColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.status == UploadStatus.QUEUED || task.status == UploadStatus.UPLOADING) {
                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel upload",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (task.status == UploadStatus.FAILED) {
                IconButton(onClick = onRetry, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry upload",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (task.status == UploadStatus.UPLOADING) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = GlassHairline
            )
        }
    }
}
