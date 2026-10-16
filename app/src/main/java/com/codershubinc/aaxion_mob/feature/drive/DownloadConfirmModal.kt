package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * Responsive confirmation dialog shown before downloading non-previewable files.
 * Automatically adapts dimensions, icon scales, and padding for optimal tablet and phone UX.
 */
@Composable
fun DownloadConfirmModal(
    file: FileItem,
    serverUri: String,
    token: String,
    onDismiss: () -> Unit,
    onConfirmDownload: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600

    val accentColor = LocalAccentColor.current
    val fileStyle = resolveFileStyle(file)
    val cardShape = RoundedCornerShape(if (isTablet) 20.dp else 22.dp)
    val modalWidth = if (isTablet) 320.dp else (screenWidthDp * 0.85f).dp.coerceIn(280.dp, 350.dp)

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(modalWidth)
                    .clip(cardShape)
                    .background(GlassBrushes.glassSurface(accent = accentColor, isHighlighted = false))
                    .border(1.dp, GlassBrushes.specularBorder(accentColor.copy(alpha = 0.40f)), cardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent dismiss when tapping inside the modal card
                    )
                    .padding(if (isTablet) 18.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Visual Icon in Squircle Container
                val iconBoxSize = if (isTablet) 46.dp else 50.dp
                val iconSize = if (isTablet) 24.dp else 26.dp
                Box(
                    modifier = Modifier
                        .size(iconBoxSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(fileStyle.containerBrush)
                        .border(0.8.dp, fileStyle.tint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    FileVisualIcon(
                        fileStyle = fileStyle,
                        iconSize = iconSize
                    )
                }

                Spacer(modifier = Modifier.height(if (isTablet) 10.dp else 12.dp))

                // Title
                Text(
                    text = "Download File?",
                    color = Color.White,
                    fontSize = if (isTablet) 16.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Description
                Text(
                    text = "This file cannot be previewed in-app. Would you like to download it to your device?",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 14.dp))

                // File Details Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                        .border(0.75.dp, GlassHairline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.displayName(28),
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = IntentUtils.formatFileSize(file.size),
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        if (file.extension.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(0.6.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = file.extension.uppercase().take(5),
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Cancel and Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        isPrimary = false,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f)
                    )

                    GlassButton(
                        text = "Download",
                        onClick = onConfirmDownload,
                        isPrimary = true,
                        icon = Icons.Default.Download,
                        accentColor = accentColor,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
