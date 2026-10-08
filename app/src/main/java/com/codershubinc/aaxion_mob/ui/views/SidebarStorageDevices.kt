package com.codershubinc.aaxion_mob.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.models.StorageInfoResponse
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.Fetcher
import com.codershubinc.aaxion_mob.utils.IntentUtils
import java.util.Locale

@Composable
fun SidebarStorageDevices(
    serverUri: String,
    token: String,
    onSelectStorageDevice: (path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { com.codershubinc.aaxion_mob.auth.TokenManager.getInstance(context) }

    LaunchedEffect(serverUri, token, refreshKey) {
        if (serverUri.isNotBlank() && token.isNotBlank()) {
            isLoading = true
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STORAGE DEVICES",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            IconButton(
                onClick = { refreshKey++ },
                modifier = Modifier.size(20.dp)
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
                modifier = Modifier.fillMaxWidth().height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AaxionPrimary, strokeWidth = 1.5.dp, modifier = Modifier.size(14.dp))
            }
        } else if (storageInfo != null) {
            val info = storageInfo!!
            val mainProgress = (info.usagePercentage / 100f).toFloat().coerceIn(0f, 1f)

            // Primary System Drive Tile (Clickable -> Navigates to System Root "/")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AaxionCardBg)
                    .border(1.dp, AaxionBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelectStorageDevice("/") }
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(AaxionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "System Drive",
                                tint = AaxionPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Main Drive",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Indicator badge for system storage
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(AaxionPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "SYS",
                                        color = AaxionPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.0f", info.usagePercentage)}%",
                            color = AaxionPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF141414))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = mainProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(AaxionPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${IntentUtils.formatFileSize(info.used)} / ${IntentUtils.formatFileSize(info.total)}",
                        color = TextTertiary,
                        fontSize = 9.sp
                    )
                }
            }

            // External Storage Devices Tiles (Clickable -> Navigates to dev.mountPoint)
            val externalDevices = info.externalDevices ?: emptyList()
            externalDevices.forEach { dev ->
                Spacer(modifier = Modifier.height(6.dp))
                val devProgress = (dev.usagePercentage / 100f).toFloat().coerceIn(0f, 1f)
                val targetPath = dev.mountPoint.ifBlank { "/" }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AaxionCardBg)
                        .border(1.dp, AaxionBorder, RoundedCornerShape(8.dp))
                        .clickable { onSelectStorageDevice(targetPath) }
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(StreamerOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Usb,
                                    contentDescription = "External Storage Device",
                                    tint = StreamerOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dev.device.substringAfterLast("/").ifBlank { "External Drive" },
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Indicator badge for external device storage
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(StreamerOrange.copy(alpha = 0.2f))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "EXT",
                                            color = StreamerOrange,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.0f", dev.usagePercentage)}%",
                                color = StreamerOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF141414))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = devProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(StreamerOrange)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${IntentUtils.formatFileSize(dev.used)} / ${IntentUtils.formatFileSize(dev.total)}",
                            color = TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
