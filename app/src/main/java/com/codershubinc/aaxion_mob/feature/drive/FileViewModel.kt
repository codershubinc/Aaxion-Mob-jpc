package com.codershubinc.aaxion_mob.feature.drive

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.Fetcher
import kotlinx.coroutines.launch

enum class FileCategory {
    ALL, VIDEOS, AUDIO, IMAGES, DOCUMENTS
}

enum class SortField(val displayName: String) {
    NAME("Name"),
    SIZE("Size"),
    TYPE("Type")
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
    var sortField by mutableStateOf(SortField.NAME)
    var sortAscending by mutableStateOf(true)

    private val pathHistory = mutableListOf<String>()

    fun fetchFiles(
        baseUrl: String,
        token: String,
        path: String = "/",
        tokenManager: TokenManager? = null
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                val response = if (tokenManager != null) {
                    tokenManager.withAuthRetry(baseUrl, token) { auth ->
                        apiService.listFiles(path, "Bearer $auth")
                    }
                } else {
                    apiService.listFiles(path, "Bearer $token")
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
        tokenManager: TokenManager? = null
    ) {
        if (currentPath != path) {
            pathHistory.add(currentPath)
            fetchFiles(baseUrl, token, path, tokenManager)
        }
    }

    fun navigateUp(
        baseUrl: String,
        token: String,
        tokenManager: TokenManager? = null
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

    val filteredFiles: List<FileItem> by derivedStateOf {
        var result = files
        if (searchQuery.isNotBlank()) {
            result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        val categorized = when (selectedCategory) {
            FileCategory.ALL -> result
            FileCategory.VIDEOS -> result.filter { it.isDir || it.isVideo }
            FileCategory.AUDIO -> result.filter { it.isDir || it.isAudio }
            FileCategory.IMAGES -> result.filter { it.isDir || it.isImage }
            FileCategory.DOCUMENTS -> result.filter { it.isDir || it.isDoc }
        }

        val comparator = compareByDescending<FileItem> { it.isDir }

        val sorted = when (sortField) {
            SortField.NAME -> {
                if (sortAscending) {
                    comparator.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                } else {
                    comparator.thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            }
            SortField.SIZE -> {
                if (sortAscending) {
                    comparator.thenBy { it.size }
                } else {
                    comparator.thenByDescending { it.size }
                }
            }
            SortField.TYPE -> {
                if (sortAscending) {
                    comparator.thenBy { it.extension }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                } else {
                    comparator.thenByDescending { it.extension }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            }
        }

        categorized.sortedWith(sorted)
    }

    val streamableMedia: List<FileItem> by derivedStateOf {
        files.filter { it.isVideo || it.isAudio }
    }

    val streamableVideos: List<FileItem> by derivedStateOf {
        files.filter { it.isVideo }
    }

    val streamableAudio: List<FileItem> by derivedStateOf {
        files.filter { it.isAudio }
    }

    companion object {
        fun isVideoFile(name: String): Boolean = FileItem.isVideoFile(name)
        fun isAudioFile(name: String): Boolean = FileItem.isAudioFile(name)
        fun isImageFile(name: String): Boolean = FileItem.isImageFile(name)
        fun isDocFile(name: String): Boolean = FileItem.isDocFile(name)
    }
}

