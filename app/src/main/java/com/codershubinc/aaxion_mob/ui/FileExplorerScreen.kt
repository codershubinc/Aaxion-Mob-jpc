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

@Composable
fun FileListItem(
    file: FileItem,
    baseUrl: String,
    token: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showActionModal by remember { mutableStateOf(false) }

    val isVideo = file.isVideo
    val isAudio = file.isAudio
    val isImage = file.isImage

    val encodedPath = remember(file.rawPath) {
        try {
            URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            file.rawPath
        }
    }

    val streamUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/files/stream?path=$encodedPath&tkn=$token"
    }
    val downloadUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/files/download?path=$encodedPath&tkn=$token"
    }
    val thumbUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/images/thumbnail?path=$encodedPath&tkn=$token"
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(96)
            .build()
    }

    val currentAccent = com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor.current
    val currentIconPack = com.codershubinc.aaxion_mob.ui.theme.LocalIconPack.current

    AaxionCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AaxionSurfaceVariant,
        borderColor = if (isVideo) StreamerOrange.copy(alpha = 0.25f) else AaxionBorder,
        onClick = onClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (onLongClick != null) {
                onLongClick()
            } else {
                showActionModal = true
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon or Thumbnail
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            file.isDir -> currentAccent.copy(alpha = 0.12f)
                            isVideo -> StreamerOrange.copy(alpha = 0.12f)
                            isAudio -> StreamerGreen.copy(alpha = 0.12f)
                            isImage -> StreamerPink.copy(alpha = 0.12f)
                            else -> currentAccent.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when {
                            file.isDir -> when (currentIconPack) {
                                com.codershubinc.aaxion_mob.models.IconPackTheme.MINIMAL_ROUNDED -> Icons.Default.Folder
                                com.codershubinc.aaxion_mob.models.IconPackTheme.SYSTEM_FILLED -> Icons.Default.FolderShared
                                com.codershubinc.aaxion_mob.models.IconPackTheme.OUTLINE -> Icons.Default.FolderOpen
                                com.codershubinc.aaxion_mob.models.IconPackTheme.CYBER_NEON -> Icons.Default.FolderSpecial
                            }
                            isVideo -> Icons.Default.PlayCircleOutline
                            isAudio -> Icons.Default.MusicNote
                            file.isDoc -> Icons.Default.Description
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = when {
                            file.isDir -> currentAccent
                            isVideo -> StreamerOrange
                            isAudio -> StreamerGreen
                            isImage -> StreamerPink
                            else -> currentAccent
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (file.isDir) {
                        Text(text = "Directory", color = TextTertiary, fontSize = 11.sp)
                    } else {
                        Text(text = IntentUtils.formatFileSize(file.size), color = TextTertiary, fontSize = 11.sp)
                    }

                    if (isVideo) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• VIDEO",
                            color = StreamerOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isAudio) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• AUDIO",
                            color = StreamerGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Stream / Download Action
            if (isVideo || isAudio) {
                IconButton(
                    onClick = { IntentUtils.launchStreamIntent(context, streamUrl, isVideo, title = file.name) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Stream",
                        tint = if (isVideo) StreamerOrange else StreamerGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(AaxionSurfaceHigh)
                ) {
                    if (isVideo || isAudio) {
                        DropdownMenuItem(
                            text = { Text("Stream Now", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isVideo) StreamerOrange else StreamerGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                IntentUtils.launchStreamIntent(context, streamUrl, isVideo, title = file.name)
                            }
                        )
                    }

                    if (!file.isDir) {
                        DropdownMenuItem(
                            text = { Text("Download", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = AaxionPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                IntentUtils.launchDownloadIntent(context, downloadUrl, fileName = file.name, token = token)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Copy Stream Link", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Stream URL", streamUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Stream link copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showActionModal) {
        FileActionModal(
            file = file,
            baseUrl = baseUrl,
            token = token,
            onDismiss = { showActionModal = false },
            onOpen = {
                showActionModal = false
                onClick()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileItem,
    baseUrl: String,
    token: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showActionModal by remember { mutableStateOf(false) }

    val encodedPath = remember(file.rawPath) {
        try {
            URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            file.rawPath
        }
    }

    val thumbUrl = remember(baseUrl, encodedPath, token) {
        "$baseUrl/api/v1/images/thumbnail?path=$encodedPath&tkn=$token"
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(128)
            .build()
    }

    val currentAccent = com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor.current
    val currentIconPack = com.codershubinc.aaxion_mob.ui.theme.LocalIconPack.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        showActionModal = true
                    }
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Just the Icon / Thumbnail without background tile or border
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (file.isImage) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = when {
                        file.isDir -> when (currentIconPack) {
                            com.codershubinc.aaxion_mob.models.IconPackTheme.MINIMAL_ROUNDED -> Icons.Default.Folder
                            com.codershubinc.aaxion_mob.models.IconPackTheme.SYSTEM_FILLED -> Icons.Default.FolderShared
                            com.codershubinc.aaxion_mob.models.IconPackTheme.OUTLINE -> Icons.Default.FolderOpen
                            com.codershubinc.aaxion_mob.models.IconPackTheme.CYBER_NEON -> Icons.Default.FolderSpecial
                        }
                        file.isVideo -> Icons.Default.PlayCircleOutline
                        file.isAudio -> Icons.Default.MusicNote
                        file.isDoc -> Icons.Default.Description
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = when {
                        file.isDir -> currentAccent
                        file.isVideo -> StreamerOrange
                        file.isAudio -> StreamerGreen
                        file.isDoc -> currentAccent
                        else -> TextSecondary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Just the name below
        Text(
            text = file.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showActionModal) {
        FileActionModal(
            file = file,
            baseUrl = baseUrl,
            token = token,
            onDismiss = { showActionModal = false },
            onOpen = {
                showActionModal = false
                onClick()
            }
        )
    }
}
