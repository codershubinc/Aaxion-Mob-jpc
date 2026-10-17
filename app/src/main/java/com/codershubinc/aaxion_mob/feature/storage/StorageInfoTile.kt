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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.InsetGroupedGlassCard
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Locale

@Composable
fun StorageInfoTile(
    serverUri: String,
    token: String,
    modifier: Modifier = Modifier,
    onSelectStorageDevice: ((String) -> Unit)? = null
) {
    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val accentColor = LocalAccentColor.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.codershubinc.aaxion_mob.core.data.TokenManager.getInstance(context) }

    LaunchedEffect(serverUri, token, refreshKey) {
        if (serverUri.isNotEmpty() && token.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = tokenManager.withAuthRetry(serverUri, token) { auth ->
                    service.getStorageInfo("Bearer $auth")
                }

                if (response.isSuccessful && response.body() != null) {
                    storageInfo = response.body()
                } else {
                    errorMessage = "Server returned error: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "Server connection is not established."
            isLoading = false
        }
    }

    val info = storageInfo
    val error = errorMessage

    InsetGroupedGlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aaxion Drive Storage",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (info != null) "Host Filesystem Active" else "Checking storage...",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = { refreshKey++ },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = AaxionDanger,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    color = AaxionDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        } else if (info != null) {
            val safeUsage = if (info.usagePercentage.isNaN()) 0.0 else info.usagePercentage
            val rawProgress = (safeUsage / 100f).toFloat().coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                label = "main_storage_progress"
            )

            // Semantic health color: Critical > 90%, Warning > 75%, else accentColor
            val statusTint = when {
                rawProgress >= 0.90f -> AaxionDanger
                rawProgress >= 0.75f -> AaxionWarning
                else -> accentColor
            }

            val systemVolumeShape = RoundedCornerShape(12.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(systemVolumeShape)
                    .then(
                        if (onSelectStorageDevice != null) {
                            Modifier.clickable { onSelectStorageDevice("/") }
                        } else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "System Volume",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusTint.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (rawProgress >= 0.90f) "CRITICAL" else if (rawProgress >= 0.75f) "WARNING" else "HEALTHY",
                                color = statusTint,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", safeUsage),
                        color = statusTint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Capsule Progress Bar (6.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f))
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
                                        colors = listOf(statusTint.copy(alpha = 0.65f), statusTint)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3-Metric Summary Strip (Total, Used, Available)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pillShape = RoundedCornerShape(12.dp)
                val metrics = listOf(
                    Triple("TOTAL", IntentUtils.formatFileSize(info.total), TextPrimary),
                    Triple("USED", IntentUtils.formatFileSize(info.used), statusTint),
                    Triple("AVAILABLE", IntentUtils.formatFileSize(info.available), if (rawProgress >= 0.90f) AaxionDanger else accentColor)
                )
                metrics.forEach { (label, value, tint) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(GlassSurfaceLow)
                            .border(0.5.dp, GlassHairline, pillShape)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                color = TextTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = value,
                                color = tint,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            val externalDevices = info.externalDevices ?: emptyList()
            if (externalDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = GlassHairline, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Usb,
                        contentDescription = null,
                        tint = StreamerOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXTERNAL STORAGE (${externalDevices.size})",
                        color = StreamerOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                externalDevices.forEach { device ->
                    Spacer(modifier = Modifier.height(10.dp))
                    val devSafeUsage = if (device.usagePercentage.isNaN()) 0.0 else device.usagePercentage
                    val devRawProgress = (devSafeUsage / 100f).toFloat().coerceIn(0f, 1f)
                    val devAnimatedProgress by animateFloatAsState(
                        targetValue = devRawProgress,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
                        label = "ext_storage_progress_${device.device}"
                    )
                    val devStatusTint = when {
                        devRawProgress >= 0.90f -> AaxionDanger
                        devRawProgress >= 0.75f -> AaxionWarning
                        else -> StreamerOrange
                    }
                    val devFreeBytes = (device.total - device.used).coerceAtLeast(0L)
                    val devShape = RoundedCornerShape(14.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(devShape)
                            .background(GlassBrushes.glassSurface(accent = devStatusTint, isHighlighted = false))
                            .border(0.75.dp, GlassBrushes.specularBorder(devStatusTint.copy(alpha = 0.35f)), devShape)
                            .then(
                                if (onSelectStorageDevice != null) {
                                    Modifier.clickable { onSelectStorageDevice(device.mountPoint.ifBlank { "/" }) }
                                } else Modifier
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tier 1: Icon + Name & Badge + Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(devStatusTint.copy(alpha = 0.15f))
                                    .border(0.5.dp, devStatusTint.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Usb,
                                    contentDescription = "USB Drive",
                                    tint = devStatusTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = device.device.substringAfterLast("/").ifBlank { "External Drive" },
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(devStatusTint.copy(alpha = 0.16f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "USB",
                                            color = devStatusTint,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = device.mountPoint,
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.0f%%", devSafeUsage),
                                color = devStatusTint,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tier 2: Capsule Progress Bar (6.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.07f))
                        ) {
                            val devProgressFraction = devAnimatedProgress.coerceIn(0f, 1f)
                            if (devProgressFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(devProgressFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(devStatusTint.copy(alpha = 0.65f), devStatusTint)
                                            )
                                        )
                                )
                            }
                        }

                        // Tier 3: Split Used / Free Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${IntentUtils.formatFileSize(device.used)} used",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${IntentUtils.formatFileSize(devFreeBytes)} free",
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}
