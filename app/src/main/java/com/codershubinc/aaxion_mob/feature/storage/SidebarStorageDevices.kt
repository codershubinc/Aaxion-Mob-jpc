package com.codershubinc.aaxion_mob.feature.storage

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import java.util.Locale

@Composable
fun SidebarStorageDevices(
    serverUri: String,
    token: String,
    onSelectStorageDevice: (path: String) -> Unit,
    onStorageTitleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val accentColor = LocalAccentColor.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.codershubinc.aaxion_mob.core.data.TokenManager.getInstance(context) }

    LaunchedEffect(serverUri, token, refreshKey) {
        if (serverUri.isNotBlank() && token.isNotBlank()) {
            isLoading = true
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = tokenManager.withAuthRetry(serverUri, token) { auth ->
                    service.getStorageInfo("Bearer $auth")
                }

                if (response.isSuccessful && response.body() != null) {
                    storageInfo = response.body()
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STORAGE",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (onStorageTitleClick != null) {
                            Modifier.clickable(onClick = onStorageTitleClick)
                        } else Modifier
                    )
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { refreshKey++ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Devices",
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading && storageInfo == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor, strokeWidth = 1.5.dp, modifier = Modifier.size(14.dp))
            }
        } else if (storageInfo != null) {
            val info = storageInfo!!

            // Primary System Volume (Clickable -> Navigates to System Root "/")
            StorageVolumeTile(
                title = "System Volume",
                subtitleBadge = "HOST",
                mountPoint = "/",
                icon = Icons.Rounded.Dns,
                baseTint = accentColor,
                usedBytes = info.used,
                totalBytes = info.total,
                usagePercentage = info.usagePercentage,
                onClick = { onSelectStorageDevice("/") }
            )

            // External Storage Devices
            val externalDevices = info.externalDevices ?: emptyList()
            externalDevices.forEach { dev ->
                Spacer(modifier = Modifier.height(6.dp))
                val targetPath = dev.mountPoint.ifBlank { "/" }
                StorageVolumeTile(
                    title = dev.device.substringAfterLast("/").ifBlank { "External Drive" },
                    subtitleBadge = "USB",
                    mountPoint = dev.mountPoint,
                    icon = Icons.Rounded.Usb,
                    baseTint = StreamerOrange,
                    usedBytes = dev.used,
                    totalBytes = dev.total,
                    usagePercentage = dev.usagePercentage,
                    onClick = { onSelectStorageDevice(targetPath) }
                )
            }
        }
    }
}

@Composable
private fun StorageVolumeTile(
    title: String,
    subtitleBadge: String,
    mountPoint: String,
    icon: ImageVector,
    baseTint: Color,
    usedBytes: Long,
    totalBytes: Long,
    usagePercentage: Double,
    onClick: () -> Unit
) {
    val safeUsage = if (usagePercentage.isNaN()) 0.0 else usagePercentage
    val rawProgress = (safeUsage / 100f).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "storage_progress"
    )

    // Semantic health color: Critical > 90%, Warning > 75%, else baseTint
    val statusTint = when {
        rawProgress >= 0.90f -> AaxionDanger
        rawProgress >= 0.75f -> AaxionWarning
        else -> baseTint
    }

    val freeBytes = (totalBytes - usedBytes).coerceAtLeast(0L)
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassBrushes.glassSurface(accent = statusTint, isHighlighted = false))
            .border(0.75.dp, GlassBrushes.specularBorder(statusTint.copy(alpha = 0.35f)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tier 1: Compact 26.dp Icon + Title & Badge + Right-aligned Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(statusTint.copy(alpha = 0.15f))
                    .border(0.5.dp, statusTint.copy(alpha = 0.35f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = statusTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusTint.copy(alpha = 0.16f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = subtitleBadge,
                            color = statusTint,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = mountPoint,
                    color = TextTertiary,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format(Locale.US, "%.0f%%", safeUsage),
                color = statusTint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tier 2: Slim 4.dp Capsule Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            val progressFraction = animatedProgress.coerceIn(0f, 1f)
            if (progressFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(statusTint.copy(alpha = 0.70f), statusTint)
                            )
                        )
                )
            }
        }

        // Tier 3: Compact Split Used / Free Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${IntentUtils.formatFileSize(usedBytes)} used",
                color = TextSecondary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${IntentUtils.formatFileSize(freeBytes)} free",
                color = TextTertiary,
                fontSize = 9.5.sp
            )
        }
    }
}
