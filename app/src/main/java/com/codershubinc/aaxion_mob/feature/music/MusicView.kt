package com.codershubinc.aaxion_mob.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.feature.drive.FileViewModel
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun MusicView(
    serverUri: String,
    token: String,
    viewModel: FileViewModel? = null,
    musicViewModel: MusicViewModel = viewModel(),
    tokenManager: TokenManager? = null,
    isSidebarVisible: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    isTablet: Boolean = false
) {
    val context = LocalContext.current
    val manager = remember { MediaPlayerManager.getInstance(context) }
    val playbackState by manager.state.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Fetch server music library on start
    LaunchedEffect(serverUri, token) {
        if (serverUri.isNotBlank() && token.isNotBlank()) {
            musicViewModel.fetchTracks(serverUri, token, tokenManager)
        }
    }

    val displayTracks = musicViewModel.filteredTracks

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTablet && !isSidebarVisible) {
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AaxionCardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Show Sidebar",
                        tint = StreamerGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StreamerGreen.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = StreamerGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Music Library",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (musicViewModel.isLoading) {
                        "Updating library..."
                    } else {
                        "${musicViewModel.tracks.size} tracks in your library"
                    },
                    color = TextTertiary,
                    fontSize = 11.5.sp
                )
            }

            // Quick Add action in top bar
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StreamerGreen.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Track",
                    tint = StreamerGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Refresh action
            IconButton(
                onClick = {
                    musicViewModel.fetchTracks(serverUri, token, tokenManager)
                },
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AaxionCardBg)
            ) {
                if (musicViewModel.isLoading) {
                    CircularProgressIndicator(
                        color = StreamerGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (displayTracks.isEmpty() && !musicViewModel.isLoading) {
            MusicEmptyState(
                currentPath = "Library",
                onAddTrackFromYouTube = {
                    showAddDialog = true
                },
                onNavigateToMusicFolder = if (viewModel != null) {
                    { viewModel.navigateTo(serverUri, token, "/Music", tokenManager = tokenManager) }
                } else null
            )
        } else {
            // Hero Actions (Shuffle All, Play All, Search, Add)
            MusicHeaderActions(
                trackCount = displayTracks.size,
                searchQuery = musicViewModel.searchQuery,
                onSearchQueryChange = { query ->
                    musicViewModel.searchTracks(serverUri, token, query, tokenManager)
                },
                onShuffleAll = {
                    if (displayTracks.isNotEmpty()) {
                        manager.playMusicQueue(
                            tracks = displayTracks,
                            startIndex = 0,
                            shuffle = true,
                            token = token,
                            baseUrl = serverUri
                        )
                    }
                },
                onPlayAll = {
                    if (displayTracks.isNotEmpty()) {
                        manager.playMusicQueue(
                            tracks = displayTracks,
                            startIndex = 0,
                            shuffle = false,
                            token = token,
                            baseUrl = serverUri
                        )
                    }
                },
                onAddTrack = {
                    showAddDialog = true
                },
                currentPath = "Library"
            )

            // Tracks list or grid
            if (isTablet) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = displayTracks,
                        key = { _, item -> item.id }
                    ) { index, track ->
                        val isCurrentTrack = playbackState.musicTrackId == track.id ||
                            playbackState.currentFile?.rawPath == track.filePath

                        MusicTrackCard(
                            track = track,
                            isCurrentTrack = isCurrentTrack,
                            isPlaying = isCurrentTrack && playbackState.isPlaying,
                            serverUri = serverUri,
                            token = token,
                            onClick = {
                                manager.playMusicQueue(
                                    tracks = displayTracks,
                                    startIndex = index,
                                    shuffle = false,
                                    token = token,
                                    baseUrl = serverUri
                                )
                            },
                            onPlayPauseToggle = {
                                if (isCurrentTrack) {
                                    manager.togglePlayPause()
                                } else {
                                    manager.playMusicQueue(
                                        tracks = displayTracks,
                                        startIndex = index,
                                        shuffle = false,
                                        token = token,
                                        baseUrl = serverUri
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = displayTracks,
                        key = { _, item -> item.id }
                    ) { index, track ->
                        val isCurrentTrack = playbackState.musicTrackId == track.id ||
                            playbackState.currentFile?.rawPath == track.filePath

                        MusicTrackCard(
                            track = track,
                            isCurrentTrack = isCurrentTrack,
                            isPlaying = isCurrentTrack && playbackState.isPlaying,
                            serverUri = serverUri,
                            token = token,
                            onClick = {
                                manager.playMusicQueue(
                                    tracks = displayTracks,
                                    startIndex = index,
                                    shuffle = false,
                                    token = token,
                                    baseUrl = serverUri
                                )
                            },
                            onPlayPauseToggle = {
                                if (isCurrentTrack) {
                                    manager.togglePlayPause()
                                } else {
                                    manager.playMusicQueue(
                                        tracks = displayTracks,
                                        startIndex = index,
                                        shuffle = false,
                                        token = token,
                                        baseUrl = serverUri
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Track from YouTube Dialog
    if (showAddDialog) {
        AddMusicDialog(
            isAdding = musicViewModel.isAddingTrack,
            statusMessage = musicViewModel.addStatusMessage,
            onDismiss = { showAddDialog = false },
            onAdd = { uri ->
                musicViewModel.addTrackFromUri(
                    baseUrl = serverUri,
                    token = token,
                    uri = uri,
                    tokenManager = tokenManager
                ) { success, _ ->
                    if (success) {
                        showAddDialog = false
                    }
                }
            }
        )
    }
}
