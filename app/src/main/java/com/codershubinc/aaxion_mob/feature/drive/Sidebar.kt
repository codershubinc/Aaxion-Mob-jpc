package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.R
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.feature.storage.SidebarStorageDevices
import androidx.core.net.toUri

import com.codershubinc.aaxion_mob.ui.designsystem.GlassPillIconButton

/**
 * Navigation item used inside the sidebar with squircle icon and specular border
 */
@Composable
fun TabletSidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = LocalAccentColor.current
) {
    val shape = RoundedCornerShape(12.dp)
    val iconContainerShape = RoundedCornerShape(8.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) selectedColor.copy(alpha = 0.15f) else Color.Transparent)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 0.75.dp,
                        brush = GlassBrushes.specularBorder(selectedColor.copy(alpha = 0.45f)),
                        shape = shape
                    )
                } else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(iconContainerShape)
                .background(if (isSelected) selectedColor.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f))
                .border(
                    width = 0.5.dp,
                    color = if (isSelected) selectedColor.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f),
                    shape = iconContainerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) selectedColor else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Core content of the sidebar (header, navigation links, storage devices, settings)
 */
@Composable
fun SidebarContent(
    serverUri: String,
    activeServerUri: String,
    effectiveToken: String,
    prioritizedServerUris: List<String> = emptyList(),
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    showCloseButton: Boolean = true,
    onCloseSidebar: () -> Unit = {},
    onNavigateToConnectionManager: () -> Unit = {},
    onSelectStorageDevice: (path: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val sidebarShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .width(260.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .clip(sidebarShape)
            .background(GlassBrushes.glassSurface())
            .border(
                width = 1.dp,
                brush = GlassBrushes.specularBorder(accentColor),
                shape = sidebarShape
            )
            .padding(12.dp)
    ) {
        // ZONE 1: Top Zone (Fixed)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToConnectionManager() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aaxion_logo),
                        contentDescription = "Aaxion logo",
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AAXION",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val displayHost = remember(serverUri) {
                                try {
                                    val u = serverUri.toUri()
                                    u.host ?: serverUri
                                } catch (e: Exception) {
                                    serverUri
                                }
                            }
                            Text(
                                text = displayHost.ifBlank { "No Server" },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (prioritizedServerUris.contains(activeServerUri)) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pinned IP",
                                    tint = accentColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                if (showCloseButton) {
                    GlassPillIconButton(
                        onClick = onCloseSidebar,
                        icon = Icons.AutoMirrored.Filled.MenuOpen,
                        contentDescription = "Hide Sidebar",
                        size = 36.dp,
                        iconSize = 18.dp,
                        tint = TextSecondary,
                        accentHighlight = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Items (Drive, Streamer, Storage & Hub)
            TabletSidebarItem(
                icon = Icons.Default.Folder,
                label = "My Drive",
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) },
                selectedColor = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            TabletSidebarItem(
                icon = Icons.Default.Headphones,
                label = "Music",
                isSelected = currentTab == 1,
                onClick = { onTabSelected(1) },
                selectedColor = StreamerGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            TabletSidebarItem(
                icon = Icons.Default.Storage,
                label = "Storage & Hub",
                isSelected = currentTab == 2,
                onClick = { onTabSelected(2) },
                selectedColor = accentColor
            )
        }

        // Flexible Spacer pushing storage devices and settings to the bottom
        Spacer(modifier = Modifier.weight(1f))

        // Bottom Zone: Compact Storage Devices & Settings
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = GlassHairline, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                SidebarStorageDevices(
                    serverUri = activeServerUri,
                    token = effectiveToken,
                    onSelectStorageDevice = onSelectStorageDevice,
                    onStorageTitleClick = { onTabSelected(2) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = GlassHairline, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            TabletSidebarItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == 3,
                onClick = { onTabSelected(3) },
                selectedColor = accentColor
            )
        }
    }
}

/**
 * Responsive Sidebar component that renders the slide-in drawer and optional dimmed backdrop
 */
@Composable
fun BoxScope.Sidebar(
    isVisible: Boolean,
    isDesktop: Boolean,
    serverUri: String,
    activeServerUri: String,
    effectiveToken: String,
    prioritizedServerUris: List<String> = emptyList(),
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onCloseSidebar: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    onSelectStorageDevice: (path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dimmed overlay for mobile/tablet to tap and close the sidebar
    if (!isDesktop) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCloseSidebar() }
            )
        }
    }

    // Collapsible Sidebar with rounded corners (Overlaid with smooth slide, 0 frame drops)
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(durationMillis = 200)
        ) + fadeIn(animationSpec = tween(150)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(durationMillis = 200)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier.align(Alignment.CenterStart)
    ) {
        SidebarContent(
            serverUri = serverUri,
            activeServerUri = activeServerUri,
            effectiveToken = effectiveToken,
            prioritizedServerUris = prioritizedServerUris,
            currentTab = currentTab,
            onTabSelected = { tab ->
                if (!isDesktop) onCloseSidebar()
                onTabSelected(tab)
            },
            showCloseButton = !isDesktop,
            onCloseSidebar = onCloseSidebar,
            onNavigateToConnectionManager = {
                if (!isDesktop) onCloseSidebar()
                onNavigateToConnectionManager()
            },
            onSelectStorageDevice = { path ->
                if (!isDesktop) onCloseSidebar()
                onSelectStorageDevice(path)
            }
        )
    }
}

@Composable
fun Sidebar(
    isVisible: Boolean,
    isDesktop: Boolean,
    serverUri: String,
    activeServerUri: String,
    effectiveToken: String,
    prioritizedServerUris: List<String> = emptyList(),
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onCloseSidebar: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    onSelectStorageDevice: (path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            isVisible = isVisible,
            isDesktop = isDesktop,
            serverUri = serverUri,
            activeServerUri = activeServerUri,
            effectiveToken = effectiveToken,
            prioritizedServerUris = prioritizedServerUris,
            currentTab = currentTab,
            onTabSelected = onTabSelected,
            onCloseSidebar = onCloseSidebar,
            onNavigateToConnectionManager = onNavigateToConnectionManager,
            onSelectStorageDevice = onSelectStorageDevice,
            modifier = modifier
        )
    }
}

@Composable
fun BoxScope.AppSidebar(
    isVisible: Boolean,
    isDesktop: Boolean,
    serverUri: String,
    activeServerUri: String,
    effectiveToken: String,
    prioritizedServerUris: List<String> = emptyList(),
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onCloseSidebar: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    onSelectStorageDevice: (path: String) -> Unit,
    modifier: Modifier = Modifier
) = Sidebar(
    isVisible = isVisible,
    isDesktop = isDesktop,
    serverUri = serverUri,
    activeServerUri = activeServerUri,
    effectiveToken = effectiveToken,
    prioritizedServerUris = prioritizedServerUris,
    currentTab = currentTab,
    onTabSelected = onTabSelected,
    onCloseSidebar = onCloseSidebar,
    onNavigateToConnectionManager = onNavigateToConnectionManager,
    onSelectStorageDevice = onSelectStorageDevice,
    modifier = modifier
)

@Composable
fun AppSidebar(
    isVisible: Boolean,
    isDesktop: Boolean,
    serverUri: String,
    activeServerUri: String,
    effectiveToken: String,
    prioritizedServerUris: List<String> = emptyList(),
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onCloseSidebar: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    onSelectStorageDevice: (path: String) -> Unit,
    modifier: Modifier = Modifier
) = Sidebar(
    isVisible = isVisible,
    isDesktop = isDesktop,
    serverUri = serverUri,
    activeServerUri = activeServerUri,
    effectiveToken = effectiveToken,
    prioritizedServerUris = prioritizedServerUris,
    currentTab = currentTab,
    onTabSelected = onTabSelected,
    onCloseSidebar = onCloseSidebar,
    onNavigateToConnectionManager = onNavigateToConnectionManager,
    onSelectStorageDevice = onSelectStorageDevice,
    modifier = modifier
)
