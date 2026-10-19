package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
 * Expandable AMOLED glass tile displayed in the tablet/desktop sidebar to monitor file uploads.
 */
@Composable
fun SidebarUploadTile(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val uploadManager = remember { UploadManager.getInstance(context) }
    val tasks by uploadManager.tasks.collectAsState()
    val hasActive by uploadManager.hasActiveUploads.collectAsState()
    val activeSpeed by uploadManager.activeSpeed.collectAsState()

    if (tasks.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    val activeCount = tasks.count { it.status == UploadStatus.UPLOADING || it.status == UploadStatus.QUEUED }
    val completedCount = tasks.count { it.status == UploadStatus.SUCCESS }
    val failedCount = tasks.count { it.status == UploadStatus.FAILED }
    val totalCount = tasks.size

    val overallProgress = remember(tasks) {
        if (tasks.isEmpty()) 0f
        else tasks.map { it.progress }.average().toFloat()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(GlassBrushes.glassSurface())
            .border(0.8.dp, GlassBrushes.specularBorder(accentColor), cardShape)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = !isExpanded },
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
                            .background(accentColor.copy(alpha = if (hasActive) pulseAlpha * 0.25f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasActive) Icons.Default.CloudUpload else Icons.Default.CloudDone,
                            contentDescription = "Upload Status",
                            tint = if (hasActive) accentColor else StreamerGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = if (hasActive) "Uploading ($activeCount/$totalCount)" else "Uploads Finished",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = if (hasActive) {
                            val speedStr = UploadTask.formatBytes(activeSpeed) + "/s"
                            "$speedStr • ${(overallProgress * 100).toInt()}%"
                        } else {
                            "$completedCount done" + if (failedCount > 0) ", $failedCount failed" else ""
                        }
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand uploads",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Compact Progress Bar when collapsed
            if (!isExpanded && hasActive) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = accentColor,
                    trackColor = GlassHairline
                )
            }

            // Expanded List of Tasks
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = GlassHairline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tasks list (scrollable up to max height)
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                SidebarUploadTaskItem(
                                    task = task,
                                    accentColor = accentColor,
                                    onCancel = { uploadManager.cancelUpload(task.id) },
                                    onRetry = { uploadManager.retryUpload(context, task.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasActive) {
                            TextButton(
                                onClick = { uploadManager.cancelAll() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Cancel All",
                                    color = StreamerOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            TextButton(
                                onClick = { uploadManager.clearFinished() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Clear",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarUploadTaskItem(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(6.dp)
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val statusText = when (task.status) {
                    UploadStatus.QUEUED -> "Queued (${task.formattedSize})"
                    UploadStatus.UPLOADING -> "${task.formattedUploaded} / ${task.formattedSize} • ${task.formattedSpeed}"
                    UploadStatus.SUCCESS -> "Uploaded (${task.formattedSize})"
                    UploadStatus.FAILED -> task.errorMessage ?: "Failed"
                    UploadStatus.CANCELLED -> "Cancelled"
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.status == UploadStatus.QUEUED || task.status == UploadStatus.UPLOADING) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel task",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else if (task.status == UploadStatus.FAILED) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry task",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (task.status == UploadStatus.UPLOADING) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = GlassHairline
            )
        }
    }
}
