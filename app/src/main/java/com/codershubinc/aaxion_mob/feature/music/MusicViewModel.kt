package com.codershubinc.aaxion_mob.feature.music

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.model.MusicTrack
import com.codershubinc.aaxion_mob.core.model.UpdateTrackRequest
import com.codershubinc.aaxion_mob.core.network.Fetcher
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Aaxion Music library:
 * - Fetching music tracks from GET /api/v1/music
 * - Searching music tracks from GET /api/v1/music/search?title={query}
 * - Downloading / Adding YouTube audio via POST /api/v1/music/add?uri={url}
 * - Updating track metadata via PUT /api/v1/music/update
 */
class MusicViewModel : ViewModel() {

    var tracks by mutableStateOf<List<MusicTrack>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isAddingTrack by mutableStateOf(false)
        private set

    var addStatusMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var searchQuery by mutableStateOf("")

    /**
     * Filtered tracks combining instant local filtering with server-fetched tracks.
     */
    val filteredTracks by derivedStateOf {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            tracks
        } else {
            tracks.filter { track ->
                track.title.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                track.album.contains(query, ignoreCase = true) ||
                track.filePath.substringAfterLast('/').contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * Fetch all music tracks in the user's library.
     */
    fun fetchTracks(
        baseUrl: String,
        token: String,
        tokenManager: TokenManager? = null
    ) {
        if (baseUrl.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                val response = if (tokenManager != null) {
                    tokenManager.withAuthRetry(baseUrl, token) { auth ->
                        apiService.getMusicTracks("Bearer $auth")
                    }
                } else {
                    apiService.getMusicTracks("Bearer $token")
                }

                if (response.isSuccessful) {
                    tracks = response.body() ?: emptyList()
                } else {
                    errorMessage = "Failed to load music tracks (HTTP ${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Connection error loading music"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Search tracks by title using the server endpoint.
     */
    fun searchTracks(
        baseUrl: String,
        token: String,
        query: String,
        tokenManager: TokenManager? = null
    ) {
        searchQuery = query
        if (baseUrl.isBlank() || query.isBlank()) {
            return
        }
        viewModelScope.launch {
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                val response = if (tokenManager != null) {
                    tokenManager.withAuthRetry(baseUrl, token) { auth ->
                        apiService.searchMusicTracks(query, "Bearer $auth")
                    }
                } else {
                    apiService.searchMusicTracks(query, "Bearer $token")
                }

                if (response.isSuccessful) {
                    val searchResults = response.body() ?: emptyList()
                    // Merge or update list if results returned
                    if (searchResults.isNotEmpty()) {
                        val existingIds = tracks.map { it.id }.toSet()
                        val newItems = searchResults.filter { it.id !in existingIds }
                        if (newItems.isNotEmpty()) {
                            tracks = (tracks + newItems)
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep local filtering fallback intact
            }
        }
    }

    /**
     * Downloads/adds a track or playlist from a YouTube URL to the server library.
     */
    fun addTrackFromUri(
        baseUrl: String,
        token: String,
        uri: String,
        tokenManager: TokenManager? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (baseUrl.isBlank() || uri.isBlank()) return
        viewModelScope.launch {
            isAddingTrack = true
            addStatusMessage = "Downloading audio from YouTube..."
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                val response = if (tokenManager != null) {
                    tokenManager.withAuthRetry(baseUrl, token) { auth ->
                        apiService.addMusicTrack(uri, "Bearer $auth")
                    }
                } else {
                    apiService.addMusicTrack(uri, "Bearer $token")
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val downloaded = body?.downloaded ?: 0
                    val msg = body?.message ?: "Downloaded $downloaded track(s)"
                    addStatusMessage = msg
                    // Refresh library
                    fetchTracks(baseUrl, token, tokenManager)
                    onComplete(true, msg)
                } else {
                    val err = "Download failed (HTTP ${response.code()})"
                    addStatusMessage = err
                    onComplete(false, err)
                }
            } catch (e: Exception) {
                val err = e.message ?: "Failed to add music track"
                addStatusMessage = err
                onComplete(false, err)
            } finally {
                isAddingTrack = false
            }
        }
    }

    /**
     * Update track metadata.
     */
    fun updateTrack(
        baseUrl: String,
        token: String,
        request: UpdateTrackRequest,
        tokenManager: TokenManager? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (baseUrl.isBlank()) return
        viewModelScope.launch {
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                val response = if (tokenManager != null) {
                    tokenManager.withAuthRetry(baseUrl, token) { auth ->
                        apiService.updateMusicTrack(request, "Bearer $auth")
                    }
                } else {
                    apiService.updateMusicTrack(request, "Bearer $token")
                }

                if (response.isSuccessful && response.body() != null) {
                    val updated = response.body()!!
                    tracks = tracks.map { if (it.id == updated.id) updated else it }
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
