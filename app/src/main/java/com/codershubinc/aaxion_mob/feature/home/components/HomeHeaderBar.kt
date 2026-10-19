package com.codershubinc.aaxion_mob.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.designsystem.GlassPillIconButton
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Locale

@Composable
fun HomeHeaderBar(
    greeting: String,
    displayHost: String,
    isServerOnline: Boolean?,
    serverLatencyMs: Long?,
    accentColor: Color,
    onNavigateToConnectionManager: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (isServerOnline) {
        true -> AaxionSuccess
        false -> AaxionDanger
        null -> TextTertiary
    }
    val statusLabel = when (isServerOnline) {
        true -> if (serverLatencyMs != null) "${serverLatencyMs}ms" else "Online"
        false -> "Offline"
        null -> "Checking..."
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting.uppercase(Locale.US),
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Aaxion Hub",
                color = TextPrimary,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Server Status Capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassSurfaceMedium)
                    .border(0.75.dp, GlassBrushes.specularBorder(statusColor.copy(alpha = 0.4f)), RoundedCornerShape(50))
                    .clickable(onClick = onNavigateToConnectionManager)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayHost,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPillIconButton(
                onClick = onRefresh,
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh Telemetry",
                size = 38.dp,
                iconSize = 18.dp,
                tint = TextSecondary,
                accentHighlight = accentColor
            )
            GlassPillIconButton(
                onClick = onNavigateToSettings,
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                size = 38.dp,
                iconSize = 18.dp,
                tint = TextPrimary,
                accentHighlight = accentColor
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    accentColor: Color,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
