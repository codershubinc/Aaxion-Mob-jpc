package com.codershubinc.aaxion_mob.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.FileGridItem
import com.codershubinc.aaxion_mob.ui.FileListItem
import com.codershubinc.aaxion_mob.ui.components.InteractiveBreadcrumbs
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.IntentUtils
import com.codershubinc.aaxion_mob.viewmodels.FileCategory
import com.codershubinc.aaxion_mob.viewmodels.FileViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Composable
fun DriveFilesView(
    serverUri: String,
    token: String,
    viewModel: FileViewModel,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    onOpenSettings: () -> Unit,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val tokenManager = androidx.compose.runtime.remember { com.codershubinc.aaxion_mob.auth.TokenManager.getInstance(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Search & Actions Bar (spans full width)
        Surface(
            color = AaxionSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Show sidebar toggle button if on tablet and sidebar is collapsed
                    if (isTablet && !isSidebarVisible) {
                        IconButton(
                            onClick = onToggleSidebar
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Show Sidebar",
                                tint = AaxionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    var isSearchExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    val screenWidth = LocalConfiguration.current.screenWidthDp
                    val isMobileLayout = screenWidth < 768

                    // Search bar or Icon
                    if (isMobileLayout && !isSearchExpanded && viewModel.searchQuery.isEmpty()) {
                        IconButton(
                            onClick = { isSearchExpanded = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            placeholder = { Text("Search files & folders...", color = TextMuted, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (viewModel.searchQuery.isNotEmpty() || (isMobileLayout && isSearchExpanded)) {
                                    IconButton(onClick = { 
                                        viewModel.searchQuery = "" 
                                        isSearchExpanded = false
                                    }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AaxionPrimary,
                                unfocusedBorderColor = AaxionBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Grid / List toggle
                    IconButton(
                        onClick = onToggleView
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Refresh button
                    IconButton(
                        onClick = { viewModel.fetchFiles(serverUri, token, viewModel.currentPath, tokenManager = tokenManager) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!isTablet) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Path Breadcrumb & Up Navigation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (viewModel.currentPath != "/" && viewModel.currentPath.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.navigateUp(serverUri, token, tokenManager = tokenManager) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Up",
                                tint = AaxionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    InteractiveBreadcrumbs(
                        currentPath = viewModel.currentPath,
                        itemCount = viewModel.filteredFiles.size,
                        accentColor = AaxionPrimary,
                        onNavigate = { targetPath ->
                            viewModel.navigateTo(serverUri, token, targetPath, tokenManager = tokenManager)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filter Pills
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(FileCategory.values()) { category ->
                        val isSelected = viewModel.selectedCategory == category

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AaxionPrimary.copy(alpha = 0.15f) else AaxionCardBg)
                                .clickable { viewModel.selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.name.lowercase().replaceFirstChar { it.titlecase(Locale.US) },
                                color = if (isSelected) AaxionPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Files Area (Takes all remaining screen height and width!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AaxionPrimary, strokeWidth = 2.dp)
                }
            } else if (viewModel.filteredFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (viewModel.searchQuery.isNotEmpty()) "No files match '${viewModel.searchQuery}'" else "Folder is empty",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (isGridView) {
                // Adaptive grid: clean icon + name below without background cards!
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = viewModel.filteredFiles,
                        key = { it.rawPath },
                        contentType = { if (it.isDir) "dir" else if (it.isImage) "image" else "file" }
                    ) { file ->
                        FileGridItem(
                            file = file,
                            baseUrl = serverUri,
                            token = token,
                            onClick = {
                                if (file.isDir) {
                                    viewModel.navigateTo(serverUri, token, file.rawPath, tokenManager = tokenManager)
                                } else if (file.isVideo || file.isAudio) {
                                    val encoded = try {
                                        URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                                    } catch (e: Exception) {
                                        file.rawPath
                                    }
                                    val streamUrl = "$serverUri/api/v1/files/stream?path=$encoded&tkn=$token"
                                    IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
                                }
                            }
                        )
                    }
                }
            } else {
                // Responsive full-width list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = viewModel.filteredFiles,
                        key = { it.rawPath },
                        contentType = { if (it.isDir) "dir" else if (it.isImage) "image" else "file" }
                    ) { file ->
                        FileListItem(
                            file = file,
                            baseUrl = serverUri,
                            token = token,
                            onClick = {
                                if (file.isDir) {
                                    viewModel.navigateTo(serverUri, token, file.rawPath, tokenManager = tokenManager)
                                } else if (file.isVideo || file.isAudio) {
                                    val encoded = try {
                                        URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                                    } catch (e: Exception) {
                                        file.rawPath
                                    }
                                    val streamUrl = "$serverUri/api/v1/files/stream?path=$encoded&tkn=$token"
                                    IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
