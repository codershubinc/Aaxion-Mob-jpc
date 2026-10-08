package com.codershubinc.aaxion_mob.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.utils.Fetcher
import kotlinx.coroutines.launch

enum class FileCategory {
    ALL, VIDEOS, AUDIO, IMAGES, DOCUMENTS
}

class FileViewModel : ViewModel() {
    var files by mutableStateOf<List<FileItem>>(emptyList())
        private set

    var currentPath by mutableStateOf("/")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf(FileCategory.ALL)

    private val pathHistory = mutableListOf<String>()

    fun fetchFiles(
        baseUrl: String,
        token: String,
        path: String = "/",
        tokenManager: com.codershubinc.aaxion_mob.auth.TokenManager? = null
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                var activeToken = token

                // If tokenManager is provided, make sure we have a valid non-expired access token
                if (tokenManager != null) {
                    val validToken = tokenManager.getValidAccessToken(baseUrl)
                    if (!validToken.isNullOrBlank()) {
                        activeToken = validToken
                    }
                }

                var response = apiService.listFiles(path, "Bearer $activeToken")

                // If unauthorized (401), automatically regenerate token and retry once
                if (response.code() == 401 && tokenManager != null) {
                    val refreshedToken = tokenManager.handleUnauthorizedAndRegenerate(baseUrl)
                    if (!refreshedToken.isNullOrBlank()) {
                        response = apiService.listFiles(path, "Bearer $refreshedToken")
                    }
                }

                if (response.isSuccessful) {
                    files = response.body() ?: emptyList()
                    currentPath = path
                } else {
                    errorMessage = "Error ${response.code()}: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to connect to server"
            } finally {
                isLoading = false
            }
        }
    }

    fun navigateTo(
        baseUrl: String,
        token: String,
        path: String,
        tokenManager: com.codershubinc.aaxion_mob.auth.TokenManager? = null
    ) {
        if (currentPath != path) {
            pathHistory.add(currentPath)
            fetchFiles(baseUrl, token, path, tokenManager)
        }
    }

    fun navigateUp(
        baseUrl: String,
        token: String,
        tokenManager: com.codershubinc.aaxion_mob.auth.TokenManager? = null
    ): Boolean {
        if (pathHistory.isNotEmpty()) {
            val previous = pathHistory.removeAt(pathHistory.size - 1)
            fetchFiles(baseUrl, token, previous, tokenManager)
            return true
        } else if (currentPath != "/" && currentPath.isNotEmpty()) {
            val parent = currentPath.substringBeforeLast("/", "").ifEmpty { "/" }
            fetchFiles(baseUrl, token, parent, tokenManager)
            return true
        }
        return false
    }

    val filteredFiles: List<FileItem>
        get() {
            var result = files
            if (searchQuery.isNotBlank()) {
                result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            return when (selectedCategory) {
                FileCategory.ALL -> result
                FileCategory.VIDEOS -> result.filter { it.isDir || it.isVideo }
                FileCategory.AUDIO -> result.filter { it.isDir || it.isAudio }
                FileCategory.IMAGES -> result.filter { it.isDir || it.isImage }
                FileCategory.DOCUMENTS -> result.filter { it.isDir || it.isDoc }
            }
        }

    val streamableMedia: List<FileItem>
        get() = files.filter { it.isVideo || it.isAudio }

    val streamableVideos: List<FileItem>
        get() = files.filter { it.isVideo }

    val streamableAudio: List<FileItem>
        get() = files.filter { it.isAudio }

    companion object {
        fun isVideoFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v", "ts", "3gp")

        fun isAudioFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma")

        fun isImageFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg")

        fun isDocFile(name: String): Boolean =
            name.substringAfterLast('.', "").lowercase() in listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "zip", "tar", "gz")
    }
}

