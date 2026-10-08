package com.codershubinc.aaxion_mob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.models.StorageInfoResponse
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.Fetcher
import java.util.Locale

@Composable
fun StorageInfoTile(
    serverUri: String,
    token: String,
    modifier: Modifier = Modifier
) {
    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.codershubinc.aaxion_mob.auth.TokenManager.getInstance(context) }

    LaunchedEffect(serverUri, token, refreshKey) {
        if (serverUri.isNotEmpty() && token.isNotEmpty()) {
            isLoading = true
            errorMessage = null
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val validToken = tokenManager.getValidAccessToken(serverUri) ?: token
                var response = service.getStorageInfo("Bearer $validToken")

                if (response.code() == 401) {
                    val refreshedToken = tokenManager.handleUnauthorizedAndRegenerate(serverUri)
                    if (!refreshedToken.isNullOrBlank()) {
                        response = service.getStorageInfo("Bearer $refreshedToken")
                    }
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

    AaxionCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = AaxionSurfaceVariant,
        borderColor = AaxionBorder
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AaxionPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = AaxionPrimary,
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
                    color = AaxionPrimary,
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
            val progress = (info.usagePercentage / 100f).toFloat().coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", info.usagePercentage)}% Used",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatBytes(info.used)} / ${formatBytes(info.total)}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AMOLED dark progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AaxionSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AaxionPrimaryDark, AaxionPrimary)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Free Space",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${formatBytes(info.available)} available",
                    color = AaxionPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            val externalDevices = info.externalDevices ?: emptyList()
            if (externalDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = AaxionBorder,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = null,
                        tint = StreamerOrange,
                        modifier = Modifier.size(14.dp)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = device.device.ifBlank { "External Drive" },
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = device.mountPoint,
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = "${formatBytes(device.used)} / ${formatBytes(device.total)}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    val devProgress = (device.usagePercentage / 100f).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AaxionSurface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = devProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(StreamerOrange)
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val unitIndex = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, unitIndex.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}
