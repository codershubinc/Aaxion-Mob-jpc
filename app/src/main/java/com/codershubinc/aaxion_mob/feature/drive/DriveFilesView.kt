package com.codershubinc.aaxion_mob.feature.drive

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.media.VideoProgress
import com.codershubinc.aaxion_mob.core.media.VlcPlayerManager
import com.codershubinc.aaxion_mob.core.network.buildDownloadUrl
import com.codershubinc.aaxion_mob.core.network.buildImageViewUrl
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.core.util.DownloadHelper
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.feature.drive.viwerModals.*
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    isTablet: Boolean,
    isServerOnline: Boolean?,
    isLocalConnection: Boolean,
    onOpenConnectionManager: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val tokenManager = androidx.compose.runtime.remember { TokenManager.getInstance(context) }
    var activeViewingFile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<FileItem?>(null) }
    var pendingDownloadFile by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<FileItem?>(null) }
    var pendingResumeVideo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Triple<FileItem, String, VideoProgress>?>(null) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Dynamically sync artwork when folder contents load or change
    androidx.compose.runtime.LaunchedEffect(viewModel.files) {
        if (viewModel.files.isNotEmpty()) {
            MediaPlayerManager.getInstance(context).syncArtworkIfAvailable(viewModel.files, serverUri, token)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        // Top Search & Actions Bar
        DriveTopBar(
            serverUri = serverUri,
            token = token,
            tokenManager = tokenManager,
            viewModel = viewModel,
            isGridView = isGridView,
            onToggleView = onToggleView,
            isSidebarVisible = isSidebarVisible,
            onToggleSidebar = onToggleSidebar,
            onOpenConnectionManager = onOpenConnectionManager,
            isTablet = isTablet,
            isServerOnline = isServerOnline,
            isLocalConnection = isLocalConnection
        )

        // Files Area (Wrapped in PullToRefreshBox with Obsidian Glass styling)
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = viewModel.isLoading,
            onRefresh = {
                viewModel.fetchFiles(serverUri, token, viewModel.currentPath, tokenManager = tokenManager)
            },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            indicator = {
                PulseRefreshBar(
                    isRefreshing = viewModel.isLoading,
                    distanceFraction = pullRefreshState.distanceFraction,
                    accentColor = accentColor,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            if (viewModel.isLoading && viewModel.files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CloudLoadingIcon(color = accentColor)
                }
            } else if (viewModel.filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
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
                    contentPadding = PaddingValues(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 16.dp),
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
                                } else if (file.isAudio) {
                                    val streamUrl = file.buildStreamUrl(serverUri, token)
                                    MediaPlayerManager.getInstance(context).play(
                                        file = file,
                                        streamUrl = streamUrl,
                                        token = token,
                                        baseUrl = serverUri,
                                        siblingFiles = viewModel.files
                                    )
                                } else if (file.isVideo) {
                                    val streamUrl = file.buildStreamUrl(serverUri, token)
                                    val vlcManager = VlcPlayerManager.getInstance(context)
                                    val progress = vlcManager.getProgress(file.rawPath)
                                    if (progress != null && progress.positionMs >= 5000L) {
                                        pendingResumeVideo = Triple(file, streamUrl, progress)
                                    } else {
                                        vlcManager.launchVlc(context, file, streamUrl, resume = false, token = token)
                                    }
                                } else if (canOpenFileInViewer(file)) {
                                    activeViewingFile = file
                                } else {
                                    // Prompt confirmation dialog instead of downloading automatically
                                    pendingDownloadFile = file
                                }
                            }
                        )
                    }
                }
            } else {
                // Responsive full-width list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
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
                                } else if (file.isAudio) {
                                    val streamUrl = file.buildStreamUrl(serverUri, token)
                                    MediaPlayerManager.getInstance(context).play(
                                        file = file,
                                        streamUrl = streamUrl,
                                        token = token,
                                        baseUrl = serverUri,
                                        siblingFiles = viewModel.files
                                    )
                                } else if (file.isVideo) {
                                    val streamUrl = file.buildStreamUrl(serverUri, token)
                                    val vlcManager = VlcPlayerManager.getInstance(context)
                                    val progress = vlcManager.getProgress(file.rawPath)
                                    if (progress != null && progress.positionMs >= 5000L) {
                                        pendingResumeVideo = Triple(file, streamUrl, progress)
                                    } else {
                                        vlcManager.launchVlc(context, file, streamUrl, resume = false, token = token)
                                    }
                                } else if (canOpenFileInViewer(file)) {
                                    activeViewingFile = file
                                } else {
                                    // Prompt confirmation dialog instead of downloading automatically
                                    pendingDownloadFile = file
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Built-in File Viewers Manager (Images, Text/UTF, PDF)
    FileViewerModal(
        file = activeViewingFile,
        serverUri = serverUri,
        token = token,
        onDismiss = { activeViewingFile = null }
    )

    // Download Confirmation Dialog for non-previewable files
    pendingDownloadFile?.let { file ->
        DownloadConfirmModal(
            file = file,
            serverUri = serverUri,
            token = token,
            onDismiss = { pendingDownloadFile = null },
            onConfirmDownload = {
                val downloadUrl = file.buildDownloadUrl(serverUri, token)
                DownloadHelper.enqueueDownload(context, downloadUrl, file.name, token)
                pendingDownloadFile = null
            }
        )
    }

    // Video Resume Confirmation Modal
    pendingResumeVideo?.let { (file, streamUrl, progress) ->
        VideoResumeModal(
            file = file,
            progress = progress,
            onResume = {
                VlcPlayerManager.getInstance(context).launchVlc(context, file, streamUrl, resume = true, token = token)
                pendingResumeVideo = null
            },
            onStartOver = {
                VlcPlayerManager.getInstance(context).launchVlc(context, file, streamUrl, resume = false, token = token)
                pendingResumeVideo = null
            },
            onPlayInternal = {
                MediaPlayerManager.getInstance(context).play(
                    file = file,
                    streamUrl = streamUrl,
                    token = token,
                    baseUrl = serverUri,
                    siblingFiles = viewModel.files
                )
                pendingResumeVideo = null
            },
            onInspectIsland = {
                VlcPlayerManager.getInstance(context).inspectVideo(file, streamUrl, token = token)
                pendingResumeVideo = null
            },
            onDismiss = { pendingResumeVideo = null }
        )
    }
}
