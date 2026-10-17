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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
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
import com.codershubinc.aaxion_mob.core.media.MediaPlayerManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.feature.drive.FileViewModel
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun MusicView(
    serverUri: String,
    token: String,
    viewModel: FileViewModel,
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val manager = remember { MediaPlayerManager.getInstance(context) }
    val playbackState by manager.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Filter audio files only
    val allAudioTracks = remember(viewModel.files) {
        viewModel.files.filter { it.isAudio }
    }

    val displayTracks = remember(allAudioTracks, searchQuery) {
        if (searchQuery.isBlank()) {
            allAudioTracks
        } else {
            allAudioTracks.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

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

            Column {
                Text(
                    text = "Music Library",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allAudioTracks.size} songs in ${viewModel.currentPath.ifBlank { "/" }}",
                    color = TextTertiary,
                    fontSize = 11.5.sp
                )
            }
        }

        if (allAudioTracks.isEmpty()) {
            MusicEmptyState(
                currentPath = viewModel.currentPath,
                onNavigateToMusicFolder = {
                    // Quick jump to standard music directory if available
                    viewModel.navigateTo(serverUri, token, "/Music")
                }
            )
        } else {
            // Hero Actions (Shuffle All, Play All, Search)
            MusicHeaderActions(
                trackCount = displayTracks.size,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onShuffleAll = {
                    if (displayTracks.isNotEmpty()) {
                        manager.playQueue(
                            tracks = displayTracks,
                            startIndex = 0,
                            shuffle = true,
                            token = token,
                            baseUrl = serverUri,
                            siblingFiles = viewModel.files
                        )
                    }
                },
                onPlayAll = {
                    if (displayTracks.isNotEmpty()) {
                        manager.playQueue(
                            tracks = displayTracks,
                            startIndex = 0,
                            shuffle = false,
                            token = token,
                            baseUrl = serverUri,
                            siblingFiles = viewModel.files
                        )
                    }
                },
                currentPath = viewModel.currentPath
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
                        key = { _, item -> item.rawPath }
                    ) { index, track ->
                        val isCurrentTrack = playbackState.currentFile?.rawPath == track.rawPath

                        MusicTrackCard(
                            track = track,
                            isCurrentTrack = isCurrentTrack,
                            isPlaying = isCurrentTrack && playbackState.isPlaying,
                            siblingFiles = viewModel.files,
                            serverUri = serverUri,
                            token = token,
                            onClick = {
                                manager.playQueue(
                                    tracks = displayTracks,
                                    startIndex = index,
                                    shuffle = false,
                                    token = token,
                                    baseUrl = serverUri,
                                    siblingFiles = viewModel.files
                                )
                            },
                            onPlayPauseToggle = {
                                if (isCurrentTrack) {
                                    manager.togglePlayPause()
                                } else {
                                    manager.playQueue(
                                        tracks = displayTracks,
                                        startIndex = index,
                                        shuffle = false,
                                        token = token,
                                        baseUrl = serverUri,
                                        siblingFiles = viewModel.files
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
                        key = { _, item -> item.rawPath }
                    ) { index, track ->
                        val isCurrentTrack = playbackState.currentFile?.rawPath == track.rawPath

                        MusicTrackCard(
                            track = track,
                            isCurrentTrack = isCurrentTrack,
                            isPlaying = isCurrentTrack && playbackState.isPlaying,
                            siblingFiles = viewModel.files,
                            serverUri = serverUri,
                            token = token,
                            onClick = {
                                manager.playQueue(
                                    tracks = displayTracks,
                                    startIndex = index,
                                    shuffle = false,
                                    token = token,
                                    baseUrl = serverUri,
                                    siblingFiles = viewModel.files
                                )
                            },
                            onPlayPauseToggle = {
                                if (isCurrentTrack) {
                                    manager.togglePlayPause()
                                } else {
                                    manager.playQueue(
                                        tracks = displayTracks,
                                        startIndex = index,
                                        shuffle = false,
                                        token = token,
                                        baseUrl = serverUri,
                                        siblingFiles = viewModel.files
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
