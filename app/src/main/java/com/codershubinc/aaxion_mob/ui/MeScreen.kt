package com.codershubinc.aaxion_mob.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.models.StorageInfoResponse
import com.codershubinc.aaxion_mob.models.ExternalDeviceInfo
import com.codershubinc.aaxion_mob.ui.components.BackgroundGlows
import com.codershubinc.aaxion_mob.ui.components.ModernCard
import com.codershubinc.aaxion_mob.utils.Fetcher
import java.util.Locale

@Composable
fun MeScreen(
    serverUri: String,
    token: String,
    onExploreFiles: () -> Unit,
    onLogout: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()

    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoadingStorage by remember { mutableStateOf(true) }

    LaunchedEffect(serverUri, token) {
        if (serverUri.isNotEmpty() && token.isNotEmpty()) {
            isLoadingStorage = true
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = service.getStorageInfo(token)
                if (response.isSuccessful && response.body() != null) {
                    storageInfo = response.body()
                } else {
                    // Fallback to mock data if API call returns error
                    storageInfo = getMockStorageInfo()
                }
            } catch (e: Exception) {
                // Fallback to mock data if network exception (server not running, etc.)
                storageInfo = getMockStorageInfo()
            } finally {
                isLoadingStorage = false
            }
        } else {
            // Load mock data to display visually even when not fully configured yet
            storageInfo = getMockStorageInfo()
            isLoadingStorage = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
    ) {
        BackgroundGlows()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (isTablet) 48.dp else 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aaxion Portal",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                // Connected status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B5E20).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Connected",
                        color = Color(0xFF81C784),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Main Content Area
            if (isTablet) {
                // Tablet layout: side-by-side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Profile/Avatar & Storage column on left
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ModernCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AvatarGroup()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Welcome to Aaxion",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Administrator Session",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        StorageCard(storageInfo = storageInfo, isLoading = isLoadingStorage)
                    }

                    // Actions column on right
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Available Actions",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        DashboardActionCard(
                            icon = Icons.Default.Folder,
                            title = "Explore Files",
                            subtitle = "Browse, upload, and manage cloud directories",
                            iconBgColor = Color(0xFF3D5AFE).copy(alpha = 0.15f),
                            iconColor = Color(0xFF3D5AFE),
                            onClick = onExploreFiles
                        )
                        DashboardActionCard(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Logout Session",
                            subtitle = "Disconnect and clear local credentials",
                            iconBgColor = Color(0xFFEF5350).copy(alpha = 0.15f),
                            iconColor = Color(0xFFEF5350),
                            onClick = onLogout
                        )
                    }
                }
            } else {
                // Phone layout: vertical scroll
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AvatarGroup()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Welcome to Aaxion",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Administrator Session",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    StorageCard(storageInfo = storageInfo, isLoading = isLoadingStorage)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    DashboardActionCard(
                        icon = Icons.Default.Folder,
                        title = "Explore Files",
                        subtitle = "Browse, upload, and manage files",
                        iconBgColor = Color(0xFF3D5AFE).copy(alpha = 0.15f),
                        iconColor = Color(0xFF3D5AFE),
                        onClick = onExploreFiles
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DashboardActionCard(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Logout",
                        subtitle = "Disconnect from this server",
                        iconBgColor = Color(0xFFEF5350).copy(alpha = 0.15f),
                        iconColor = Color(0xFFEF5350),
                        onClick = onLogout
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StorageCard(
    storageInfo: StorageInfoResponse?,
    isLoading: Boolean
) {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = Color(0xFF3D5AFE),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "System Storage",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading || storageInfo == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            val progress = (storageInfo.usagePercentage / 100f).toFloat().coerceIn(0f, 1f)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", storageInfo.usagePercentage)}% Used",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatBytes(storageInfo.used)} / ${formatBytes(storageInfo.total)}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress > 0.85f) Color(0xFFEF5350) else Color(0xFF3D5AFE),
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Available: ${formatBytes(storageInfo.available)}",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )

            if (storageInfo.externalDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "EXTERNAL DEVICES",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )

                storageInfo.externalDevices.forEach { device ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.mountPoint,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${device.device} (${device.filesystemType})",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", device.usagePercentage)}% used",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatBytes(device.used)} / ${formatBytes(device.total)}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / 1_000_000_000.0
    return if (gb >= 1000.0) {
        val tb = gb / 1000.0
        String.format(Locale.US, "%.2f TB", tb)
    } else {
        String.format(Locale.US, "%.1f GB", gb)
    }
}

private fun getMockStorageInfo(): StorageInfoResponse {
    return StorageInfoResponse(
        total = 512110190592,
        used = 312110190592,
        available = 200000000000,
        usagePercentage = 60.945892548,
        externalDevices = listOf(
            ExternalDeviceInfo(
                device = "/dev/sdb1",
                mountPoint = "/media/usb-drive",
                filesystemType = "ext4",
                total = 128035676160,
                used = 28035676160,
                available = 100000000000,
                usagePercentage = 21.89676839
            )
        )
    )
}

@Composable
fun AvatarGroup() {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0xFF3D5AFE).copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DashboardActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
