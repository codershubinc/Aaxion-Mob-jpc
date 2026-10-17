package com.codershubinc.aaxion_mob.feature.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard

import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageAndHubView(
    serverUri: String,
    token: String,
    masterToken: String?,
    lastRefreshTs: Long,
    isRefreshing: Boolean,
    showStorageDevices: Boolean = false,
    onSelectStorageDevice: ((String) -> Unit)? = null,
    onRefreshToken: () -> Unit,
    onOpenIpSelector: () -> Unit,
    onLogout: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Storage Tile (Spans full width with interactive storage device navigation)
        StorageInfoTile(
            serverUri = serverUri,
            token = token,
            onSelectStorageDevice = onSelectStorageDevice
        )

        // Connected Server & IP Selector Tile
        InsetGroupedGlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenIpSelector() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Connected Server IP",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (serverUri.isNotBlank()) serverUri else "Not connected",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Switch / Pin",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Session & Security Tokens (Flattened Inset Grouped Rows)
        InsetGroupedGlassCard {
            // Master Token Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Master Token",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!masterToken.isNullOrBlank()) "••••••••••••${masterToken.takeLast(6)}" else "Not available",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Used to re-generate access token every 4 hours.",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            InsetGlassDivider(startIndent = 16.dp)

            // Access Token Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Access Token (Bearer)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (token.isNotBlank()) "••••••••••••${token.takeLast(6)}" else "Not available",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Active bearer token attached to API requests.",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            InsetGlassDivider(startIndent = 16.dp)

            // Last Refreshed Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LAST REFRESHED",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lastRefreshTs > 0) dateFormat.format(Date(lastRefreshTs)) else "Upon login",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                GlassButton(
                    text = "Refresh Token Now",
                    onClick = onRefreshToken,
                    isLoading = isRefreshing,
                    enabled = !isRefreshing,
                    cornerRadius = 8.dp,
                    accentColor = accentColor
                )
            }
        }

        // Sign Out Button
        InsetGroupedGlassCard(accentHighlight = AaxionDanger) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = AaxionDanger,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sign Out & Clear Tokens",
                        color = AaxionDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Disconnects from server and deletes local credentials",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
