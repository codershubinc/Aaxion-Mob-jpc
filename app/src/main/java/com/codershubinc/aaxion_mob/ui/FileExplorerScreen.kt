package com.codershubinc.aaxion_mob.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.ui.components.files.FileGridItem
import com.codershubinc.aaxion_mob.ui.components.files.FileListItem
import com.codershubinc.aaxion_mob.ui.components.AaxionCard
import com.codershubinc.aaxion_mob.ui.components.FileActionModal
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.IntentUtils
import com.codershubinc.aaxion_mob.viewmodels.FileCategory
import com.codershubinc.aaxion_mob.viewmodels.FileViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    files: List<FileItem>,
    currentPath: String,
    baseUrl: String,
    token: String,
    onFileClick: (FileItem) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var isGridView by remember { mutableStateOf(isTablet) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }

    // Filter files based on search and category
    val displayedFiles = remember(files, searchQuery, selectedCategory) {
        var list = files
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        when (selectedCategory) {
            FileCategory.ALL -> list
            FileCategory.VIDEOS -> list.filter { it.isDir || FileViewModel.isVideoFile(it.name) }
            FileCategory.AUDIO -> list.filter { it.isDir || FileViewModel.isAudioFile(it.name) }
            FileCategory.IMAGES -> list.filter { it.isDir || FileViewModel.isImageFile(it.name) }
            FileCategory.DOCUMENTS -> list.filter { it.isDir || FileViewModel.isDocFile(it.name) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AaxionBg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AaxionSurface)
                        .statusBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            if (isSearching) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search files...", color = TextMuted) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AaxionPrimary,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Aaxion Drive",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${files.size} items",
                                            color = TextTertiary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = currentPath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                isSearching = !isSearching
                                if (!isSearching) searchQuery = ""
                            }) {
                                Icon(
                                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearching) AaxionPrimary else TextSecondary
                                )
                            }
                            IconButton(onClick = { isGridView = !isGridView }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = TextSecondary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = TextPrimary
                        )
                    )

                    // Category Filter Pills
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FileCategory.values()) { category ->
                            val isSelected = selectedCategory == category

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) AaxionPrimary.copy(alpha = 0.15f)
                                        else AaxionSurfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AaxionPrimary.copy(alpha = 0.3f) else AaxionBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = category.name.lowercase()
                                        .replaceFirstChar { it.titlecase(Locale.US) },
                                    color = if (isSelected) AaxionPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No files match '$searchQuery'" else "This directory is empty",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = displayedFiles,
                        key = { it.rawPath },
                        contentType = { if (it.isDir) "dir" else if (it.isImage) "image" else "file" }
                    ) { file ->
                        FileGridItem(
                            file = file,
                            baseUrl = baseUrl,
                            token = token,
                            onClick = {
                                if (file.isVideo || file.isAudio) {
                                    val encoded = try {
                                        URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                                    } catch (e: Exception) {
                                        file.rawPath
                                    }
                                    val streamUrl = "$baseUrl/api/v1/files/stream?path=$encoded&tkn=$token"
                                    IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
                                } else {
                                    onFileClick(file)
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = displayedFiles,
                        key = { it.rawPath },
                        contentType = { if (it.isDir) "dir" else if (it.isImage) "image" else "file" }
                    ) { file ->
                        FileListItem(
                            file = file,
                            baseUrl = baseUrl,
                            token = token,
                            onClick = {
                                if (file.isVideo || file.isAudio) {
                                    val encoded = try {
                                        URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                                    } catch (e: Exception) {
                                        file.rawPath
                                    }
                                    val streamUrl = "$baseUrl/api/v1/files/stream?path=$encoded&tkn=$token"
                                    IntentUtils.launchStreamIntent(context, streamUrl, file.isVideo, title = file.name)
                                } else {
                                    onFileClick(file)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

