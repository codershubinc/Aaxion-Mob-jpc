package com.codershubinc.aaxion_mob.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.designsystem.GlassCard
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun BentoWorkspaceGrid(
    accentColor: Color,
    displayHost: String,
    isServerOnline: Boolean?,
    onNavigateToDrive: (path: String?) -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "WORKSPACES",
            actionText = "Browse Root",
            accentColor = accentColor,
            onActionClick = { onNavigateToDrive("/") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoWorkspaceTile(
                title = "My Drive",
                subtitle = "Explore host filesystem",
                badge = "FILES",
                icon = Icons.Default.Folder,
                tint = accentColor,
                onClick = { onNavigateToDrive(null) },
                modifier = Modifier.weight(1f)
            )
            BentoWorkspaceTile(
                title = "Music Library",
                subtitle = "Stream audio & playlists",
                badge = "AUDIO",
                icon = Icons.Default.Headphones,
                tint = StreamerGreen,
                onClick = onNavigateToMusic,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoWorkspaceTile(
                title = "Storage & Hub",
                subtitle = "Volumes & auth tokens",
                badge = "SYSTEM",
                icon = Icons.Default.Storage,
                tint = StreamerPurple,
                onClick = onNavigateToStorage,
                modifier = Modifier.weight(1f)
            )
            BentoWorkspaceTile(
                title = "Network & Host",
                subtitle = displayHost,
                badge = if (isServerOnline == true) "ACTIVE" else "SETUP",
                icon = Icons.Default.Dns,
                tint = StreamerOrange,
                onClick = onNavigateToConnectionManager,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BentoWorkspaceTile(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(118.dp),
        cornerRadius = 20.dp,
        accentHighlight = tint,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.2f),
                            radius = size.maxDimension * 0.65f
                        )
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.copy(alpha = 0.16f))
                        .border(0.6.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(tint.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = tint,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
