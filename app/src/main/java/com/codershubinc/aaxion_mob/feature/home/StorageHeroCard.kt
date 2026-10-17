package com.codershubinc.aaxion_mob.feature.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.GlassCard
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Locale

@Composable
internal fun StorageHeroCard(
    storageInfo: StorageInfoResponse?,
    isLoading: Boolean,
    accentColor: Color,
    onOpenStorageHub: () -> Unit,
    onSelectMountPoint: (String) -> Unit
) {
    val usagePct = storageInfo?.usagePercentage ?: 0.0
    val rawProgress = (usagePct / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "hero_storage_bar"
    )

    val healthTint = when {
        rawProgress >= 0.90f -> AaxionDanger
        rawProgress >= 0.75f -> AaxionWarning
        else -> accentColor
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        accentHighlight = healthTint,
        isHighlighted = true,
        contentPadding = PaddingValues(18.dp),
        onClick = onOpenStorageHub
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(healthTint.copy(alpha = 0.18f))
                        .border(0.75.dp, healthTint.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = healthTint,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "System Storage",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (storageInfo != null) {
                            "${IntentUtils.formatFileSize(storageInfo.available)} available of ${IntentUtils.formatFileSize(storageInfo.total)}"
                        } else if (isLoading) {
                            "Syncing volume telemetry..."
                        } else {
                            "Tap to view storage details"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (storageInfo != null) {
                Text(
                    text = String.format(Locale.US, "%.0f%%", storageInfo.usagePercentage),
                    color = healthTint,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            } else if (isLoading) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Capsule Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(healthTint.copy(alpha = 0.65f), healthTint)
                        )
                    )
            )
        }

        // Mounted Volume Quick-Jump Pills
        if (storageInfo != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VolumeQuickPill(
                    label = "Host (/)",
                    usedPct = String.format(Locale.US, "%.0f%%", storageInfo.usagePercentage),
                    icon = Icons.Default.Folder,
                    tint = accentColor,
                    onClick = { onSelectMountPoint("/") }
                )

                storageInfo.externalDevices?.take(2)?.forEach { ext ->
                    val name = ext.device.substringAfterLast('/').ifBlank { "USB" }
                    VolumeQuickPill(
                        label = name,
                        usedPct = String.format(Locale.US, "%.0f%%", ext.usagePercentage),
                        icon = Icons.Default.Usb,
                        tint = StreamerOrange,
                        onClick = { onSelectMountPoint(ext.mountPoint.ifBlank { "/" }) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun VolumeQuickPill(
    label: String,
    usedPct: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.6.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = usedPct,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
