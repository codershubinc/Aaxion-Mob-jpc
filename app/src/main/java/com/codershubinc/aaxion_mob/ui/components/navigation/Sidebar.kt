package com.codershubinc.aaxion_mob.ui.components.navigation

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import com.codershubinc.aaxion_mob.ui.views.SidebarStorageDevices

/**
 * Tablet / Desktop navigation item used inside the sidebar
 */
@Composable
fun TabletSidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AaxionPrimary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) AaxionPrimary else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AaxionSurface)
            .border(1.dp, AaxionBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Header & Nav
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Hide Sidebar button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.aaxion_logo),
                        contentDescription = "Aaxion logo",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onNavigateToConnectionManager() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AAXION",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayHost = remember(serverUri) {
                                try {
                                    val u = android.net.Uri.parse(serverUri)
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
                                    tint = AaxionPrimary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                if (showCloseButton) {
                    IconButton(
                        onClick = onCloseSidebar,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                            contentDescription = "Hide Sidebar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Items
            TabletSidebarItem(
                icon = Icons.Default.Folder,
                label = "My Drive",
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) }
            )
        }

        // Bottom Section: Storage Devices Tiles & Settings Icon
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            HorizontalDivider(color = AaxionBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Storage Device Tiles directly at bottom of sidebar!
            SidebarStorageDevices(
                serverUri = activeServerUri,
                token = effectiveToken,
                onSelectStorageDevice = onSelectStorageDevice
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AaxionBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Settings Action Icon at Bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(3) }
                    .background(if (currentTab == 3) AaxionPrimary.copy(alpha = 0.12f) else Color.Transparent)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (currentTab == 3) AaxionPrimary else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Settings",
                    color = if (currentTab == 3) AaxionPrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium
                )
            }
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
            onTabSelected = onTabSelected,
            showCloseButton = !isDesktop,
            onCloseSidebar = onCloseSidebar,
            onNavigateToConnectionManager = onNavigateToConnectionManager,
            onSelectStorageDevice = onSelectStorageDevice
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
