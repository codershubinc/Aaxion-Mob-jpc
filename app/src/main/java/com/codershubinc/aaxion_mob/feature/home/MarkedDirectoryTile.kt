package com.codershubinc.aaxion_mob.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.media.DirectoryMediaType
import com.codershubinc.aaxion_mob.ui.designsystem.GlassCard
import com.codershubinc.aaxion_mob.ui.designsystem.GlassDialog
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
internal fun MarkedDirectoryTile(
    path: String,
    mediaType: DirectoryMediaType,
    isAvailable: Boolean?,
    itemCount: Int?,
    onClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showUnmarkDialog by remember { mutableStateOf(false) }

    val tileTint = when (mediaType) {
        DirectoryMediaType.MOVIES -> StreamerOrange
        DirectoryMediaType.SERIES -> StreamerPurple
        else -> StreamerGreen
    }

    val typeBadgeText = when (mediaType) {
        DirectoryMediaType.MOVIES -> "MOVIES"
        DirectoryMediaType.SERIES -> "SERIES"
        else -> "MEDIA"
    }

    val folderName = remember(path) {
        val clean = path.trimEnd('/')
        clean.substringAfterLast('/').ifBlank { "Root" }
    }

    val parentPath = remember(path) {
        val clean = path.trimEnd('/')
        val parent = clean.substringBeforeLast('/', "")
        if (parent.isBlank()) "/" else parent
    }

    val infiniteTransition = rememberInfiniteTransition(label = "avail_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    GlassCard(
        modifier = modifier.height(122.dp),
        cornerRadius = 20.dp,
        accentHighlight = tileTint.copy(alpha = if (isAvailable == false) 0.25f else 0.45f),
        contentPadding = PaddingValues(13.dp),
        onClick = onClick,
        onLongClick = if (onRemoveClick != null) { { showUnmarkDialog = true } } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isAvailable == false) Color(0xFFEF4444).copy(alpha = 0.12f)
                                else tileTint.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.18f),
                            radius = size.maxDimension * 0.65f
                        )
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Media Type Icon & Pill Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tileTint.copy(alpha = 0.16f))
                            .border(0.6.dp, tileTint.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (mediaType) {
                                DirectoryMediaType.MOVIES -> Icons.Default.Movie
                                DirectoryMediaType.SERIES -> Icons.Default.Tv
                                else -> Icons.Default.Folder
                            },
                            contentDescription = typeBadgeText,
                            tint = tileTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(tileTint.copy(alpha = 0.14f))
                            .border(0.5.dp, tileTint.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeBadgeText,
                            color = tileTint,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                // Availability Status Indicator Pill
                val statusTint = when (isAvailable) {
                    true -> Color(0xFF10B981)
                    false -> Color(0xFFEF4444)
                    null -> TextTertiary
                }
                val statusText = when (isAvailable) {
                    true -> if (itemCount != null) "$itemCount items" else "ONLINE"
                    false -> "OFFLINE"
                    null -> "CHECKING"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusTint.copy(alpha = 0.12f))
                        .border(0.5.dp, statusTint.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAvailable == true) statusTint.copy(alpha = pulseAlpha)
                                    else statusTint
                                )
                        )
                        Text(
                            text = statusText,
                            color = statusTint,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // Bottom Area: Folder Name, Subtitle path/count, and navigation arrow
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = folderName,
                        color = TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open Directory",
                        tint = if (isAvailable == false) TextTertiary.copy(alpha = 0.5f) else TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Text(
                    text = if (isAvailable == false) {
                        "Unmounted or path missing"
                    } else {
                        parentPath
                    },
                    color = if (isAvailable == false) Color(0xFFF87171) else TextSecondary,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showUnmarkDialog && onRemoveClick != null) {
        GlassDialog(
            onDismissRequest = { showUnmarkDialog = false },
            title = "Unmark Directory",
            accentHighlight = tileTint,
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnmarkDialog = false
                        onRemoveClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Unmark", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnmarkDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            Text(
                text = "Remove '$folderName' from your marked directories? This will remove its shortcut from the home screen.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
