package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Locale

/**
 * Top bar component for Aaxion Drive explorer.
 * Streamlined 2-row layout with integrated breadcrumbs header,
 * expandable search bar, and capsule-style sort/filter chips.
 */
@Composable
fun DriveTopBar(
    serverUri: String,
    token: String,
    tokenManager: TokenManager,
    viewModel: FileViewModel,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    onOpenConnectionManager: (() -> Unit)? = null,
    onUploadClick: (() -> Unit)? = null,
    onCreateFolderClick: (() -> Unit)? = null,
    isTablet: Boolean,
    isServerOnline: Boolean?,
    isLocalConnection: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val topBarShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    var isSearchExpanded by remember { mutableStateOf(viewModel.searchQuery.isNotBlank()) }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(topBarShape)
            .background(GlassBrushes.glassSurface())
            .border(1.dp, GlassBrushes.specularBorder(), topBarShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Row 1: Sidebar Toggle (if collapsed), Breadcrumb Navigation, Grid/List Toggle, and Connection Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Show sidebar toggle button if sidebar is collapsed
                if (!isSidebarVisible) {
                    IconButton(
                        onClick = onToggleSidebar,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Show Sidebar",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Interactive Breadcrumbs taking the flexible center space
                Box(modifier = Modifier.weight(1f)) {
                    InteractiveBreadcrumbs(
                        currentPath = viewModel.currentPath,
                        itemCount = viewModel.filteredFiles.size,
                        accentColor = accentColor,
                        onNavigate = { targetPath ->
                            viewModel.navigateTo(serverUri, token, targetPath, tokenManager = tokenManager)
                        },
                        onNavigateUp = {
                            viewModel.navigateUp(serverUri, token, tokenManager = tokenManager)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Create Folder button
                if (onCreateFolderClick != null) {
                    IconButton(
                        onClick = onCreateFolderClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "New Folder",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Upload Files button
                if (onUploadClick != null) {
                    IconButton(
                        onClick = onUploadClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Files",
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Grid / List toggle
                IconButton(
                    onClick = onToggleView,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Interactive Connection Status Pill (launches Connection Manager on tap)
                Spacer(modifier = Modifier.width(4.dp))
                val statusColor = if (isServerOnline == true) {
                    if (isLocalConnection) accentColor else StreamerOrange
                } else {
                    TextMuted
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(0.8.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .clickable(enabled = onOpenConnectionManager != null) { onOpenConnectionManager?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isServerOnline == true) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Connection Status",
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Search Bar (animated visibility when search is toggled)
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchBar(
                    query = viewModel.searchQuery,
                    onQueryChange = { viewModel.searchQuery = it },
                    onClear = {
                        viewModel.searchQuery = ""
                        isSearchExpanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Sort Dropdown Pill, Search Toggle Pill & Category Filter Chips
            var showSortMenu by remember { mutableStateOf(false) }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Dropdown Pill
                item {
                    Box {
                        val sortShape = RoundedCornerShape(50)
                        Row(
                            modifier = Modifier
                                .clip(sortShape)
                                .background(GlassBrushes.glassSurface(accent = accentColor, isHighlighted = true))
                                .border(0.8.dp, GlassBrushes.specularBorder(accentColor), sortShape)
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = viewModel.sortField.displayName,
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (viewModel.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = if (viewModel.sortAscending) "Ascending" else "Descending",
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        GlassContextMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            accentHighlight = accentColor
                        ) {
                            SortField.values().forEach { field ->
                                val isSelected = viewModel.sortField == field
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = field.displayName,
                                                color = if (isSelected) accentColor else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = if (viewModel.sortAscending) "Ascending ↑" else "Descending ↓",
                                                    color = accentColor,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        val icon = when (field) {
                                            SortField.NAME -> Icons.Default.SortByAlpha
                                            SortField.SIZE -> Icons.Default.Storage
                                            SortField.TYPE -> Icons.Default.Category
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        if (viewModel.sortField == field) {
                                            viewModel.sortAscending = !viewModel.sortAscending
                                        } else {
                                            viewModel.sortField = field
                                            viewModel.sortAscending = true
                                        }
                                        showSortMenu = false
                                    }
                                )
                            }
                            GlassMenuDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (viewModel.sortAscending) "Order: Ascending ↑" else "Order: Descending ↓",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (viewModel.sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.sortAscending = !viewModel.sortAscending
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // Search Toggle Pill (Next to Sort button)
                item {
                    val searchShape = RoundedCornerShape(50)
                    val isSearching = isSearchExpanded || viewModel.searchQuery.isNotBlank()
                    Box(
                        modifier = Modifier
                            .clip(searchShape)
                            .background(
                                if (isSearching) GlassBrushes.glassSurface(accent = accentColor, isHighlighted = true)
                                else GlassBrushes.glassSurface()
                            )
                            .border(
                                width = 0.8.dp,
                                brush = GlassBrushes.specularBorder(if (isSearching) accentColor else null),
                                shape = searchShape
                            )
                            .clickable {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) {
                                    viewModel.searchQuery = ""
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearching) accentColor else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            if (viewModel.searchQuery.isNotBlank()) {
                                Text(
                                    text = viewModel.searchQuery,
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Category Filter Chips
                items(FileCategory.values()) { category ->
                    val isSelected = viewModel.selectedCategory == category

                    FilterChip(
                        label = category.name.lowercase().replaceFirstChar { it.titlecase(Locale.US) },
                        isSelected = isSelected,
                        onClick = { viewModel.selectedCategory = category }
                    )
                }
            }
        }
    }
}
