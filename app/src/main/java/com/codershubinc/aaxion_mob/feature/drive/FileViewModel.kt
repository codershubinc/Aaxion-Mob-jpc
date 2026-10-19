package com.codershubinc.aaxion_mob.feature.drive

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.media.DirectoryMediaType
import com.codershubinc.aaxion_mob.core.media.MediaDirectoryManager
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.network.OmdbClient
import kotlinx.coroutines.flow.*
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
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val filesFlow: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQueryFlow: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FileCategory.ALL)
    val selectedCategoryFlow: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    private val _sortField = MutableStateFlow(SortField.NAME)
    val sortFieldFlow: StateFlow<SortField> = _sortField.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscendingFlow: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _currentDirectoryMediaType = MutableStateFlow(DirectoryMediaType.NONE)
    val currentDirectoryMediaType: StateFlow<DirectoryMediaType> = _currentDirectoryMediaType.asStateFlow()

    var files: List<FileItem>
        get() = _files.value
        private set(value) { _files.value = value }

    var searchQuery: String
        get() = _searchQuery.value
        set(value) { _searchQuery.value = value }

    var selectedCategory: FileCategory
        get() = _selectedCategory.value
        set(value) { _selectedCategory.value = value }

    var sortField: SortField
        get() = _sortField.value
        set(value) { _sortField.value = value }

    var sortAscending: Boolean
        get() = _sortAscending.value
        set(value) { _sortAscending.value = value }

    var currentPath by mutableStateOf("/")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val pathHistory = mutableStateListOf<String>()

    val canNavigateUp: Boolean
        get() = pathHistory.isNotEmpty() || (currentPath != "/" && currentPath.isNotEmpty())

    private var lastBaseUrl: String = ""
    private var lastToken: String = ""

    fun fetchFiles(
        baseUrl: String,
        token: String,
        path: String = "/",
        tokenManager: TokenManager? = null,
        context: Context? = null
    ) {
        if (baseUrl.isNotBlank()) lastBaseUrl = baseUrl
        if (token.isNotBlank()) lastToken = token

        val effectiveBase = baseUrl.ifBlank { lastBaseUrl }
        val effectiveTok = token.ifBlank { lastToken }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val apiService = Fetcher.getInstance().getService(effectiveBase)
                val response = if (tokenManager != null) {
                    tokenManager.withAuthRetry(effectiveBase, effectiveTok) { auth ->
                        apiService.listFiles(path, "Bearer $auth")
                    }
                } else {
                    apiService.listFiles(path, "Bearer $effectiveTok")
                }

                if (response.isSuccessful) {
                    files = response.body() ?: emptyList()
                    currentPath = path
                    if (context != null) {
                        updateCurrentDirectoryType(context)
                    }
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

    fun updateCurrentDirectoryType(context: Context) {
        val resolved = MediaDirectoryManager.getInstance(context).resolveDirectoryType(currentPath)
        _currentDirectoryMediaType.value = resolved
        if (resolved != DirectoryMediaType.NONE && files.isNotEmpty()) {
            triggerOmdbSync(context)
        }
    }

    fun setDirectoryMediaType(context: Context, path: String, type: DirectoryMediaType) {
        MediaDirectoryManager.getInstance(context).setDirectoryType(path, type)
        _currentDirectoryMediaType.value = MediaDirectoryManager.getInstance(context).resolveDirectoryType(currentPath)
        if (_currentDirectoryMediaType.value != DirectoryMediaType.NONE) {
            triggerOmdbSync(context)
        }
    }

    fun markCurrentDirectoryAs(context: Context, type: DirectoryMediaType) {
        setDirectoryMediaType(context, currentPath, type)
    }

    fun triggerOmdbSync(context: Context) {
        val dirType = _currentDirectoryMediaType.value
        if (dirType != DirectoryMediaType.NONE && files.isNotEmpty()) {
            viewModelScope.launch {
                OmdbClient.syncDirectory(context, files, dirType)
            }
        }
    }

    suspend fun createDirectory(
        baseUrl: String,
        token: String,
        folderName: String,
        tokenManager: TokenManager? = null,
        context: Context? = null
    ): Boolean {
        val cleanName = folderName.trim().trim('/')
        if (cleanName.isBlank()) return false
        val targetPath = if (currentPath.endsWith("/")) "$currentPath$cleanName" else "$currentPath/$cleanName"

        val effectiveBase = baseUrl.ifBlank { lastBaseUrl }
        var effectiveTok = token.ifBlank { lastToken }
        if (tokenManager != null) {
            val activeAccess = tokenManager.accessTokenState.value
            if (!activeAccess.isNullOrBlank()) effectiveTok = activeAccess
        }

        return try {
            val service = Fetcher.getInstance().getService(effectiveBase)
            var response = service.createDirectory(targetPath, "Bearer $effectiveTok")
            if (response.code() == 401 && tokenManager != null) {
                val refreshed = tokenManager.refreshAccessToken(effectiveBase)
                if (refreshed) {
                    val newTok = tokenManager.accessTokenState.value ?: effectiveTok
                    response = service.createDirectory(targetPath, "Bearer $newTok")
                }
            }
            if (response.isSuccessful || response.code() == 201 || response.code() == 200) {
                fetchFiles(effectiveBase, effectiveTok, currentPath, tokenManager, context)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun navigateTo(
        baseUrl: String,
        token: String,
        path: String,
        tokenManager: TokenManager? = null
    ) {
        if (currentPath != path) {
            if (pathHistory.isEmpty() || pathHistory.last() != currentPath) {
                pathHistory.add(currentPath)
            }
            fetchFiles(baseUrl, token, path, tokenManager)
        }
    }

    fun navigateUp(
        baseUrl: String,
        token: String,
        tokenManager: TokenManager? = null
    ): Boolean {
        while (pathHistory.isNotEmpty() && pathHistory.last() == currentPath) {
            pathHistory.removeAt(pathHistory.size - 1)
        }

        val effectiveBase = baseUrl.ifBlank { lastBaseUrl }
        val effectiveTok = token.ifBlank { lastToken }

        if (pathHistory.isNotEmpty()) {
            val previous = pathHistory.removeAt(pathHistory.size - 1)
            fetchFiles(effectiveBase, effectiveTok, previous, tokenManager)
            return true
        } else if (currentPath != "/" && currentPath.isNotEmpty()) {
            val parent = currentPath.substringBeforeLast("/", "").ifEmpty { "/" }
            fetchFiles(effectiveBase, effectiveTok, parent, tokenManager)
            return true
        }
        return false
    }

    val filteredFilesFlow: StateFlow<List<FileItem>> = combine(
        _files,
        _searchQuery,
        _selectedCategory,
        _sortField,
        _sortAscending
    ) { filesList, query, category, field, ascending ->
        filterAndSortFiles(filesList, query, category, field, ascending)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val streamableMediaFlow: StateFlow<List<FileItem>> = _files.map { list ->
        list.filter { it.isVideo || it.isAudio }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val streamableVideosFlow: StateFlow<List<FileItem>> = _files.map { list ->
        list.filter { it.isVideo }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val streamableAudioFlow: StateFlow<List<FileItem>> = _files.map { list ->
        list.filter { it.isAudio }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _filteredFiles = mutableStateOf<List<FileItem>>(emptyList())
    val filteredFiles: List<FileItem> by _filteredFiles

    private val _streamableMedia = mutableStateOf<List<FileItem>>(emptyList())
    val streamableMedia: List<FileItem> by _streamableMedia

    private val _streamableVideos = mutableStateOf<List<FileItem>>(emptyList())
    val streamableVideos: List<FileItem> by _streamableVideos

    private val _streamableAudio = mutableStateOf<List<FileItem>>(emptyList())
    val streamableAudio: List<FileItem> by _streamableAudio

    init {
        viewModelScope.launch {
            filteredFilesFlow.collect { _filteredFiles.value = it }
        }
        viewModelScope.launch {
            streamableMediaFlow.collect { _streamableMedia.value = it }
        }
        viewModelScope.launch {
            streamableVideosFlow.collect { _streamableVideos.value = it }
        }
        viewModelScope.launch {
            streamableAudioFlow.collect { _streamableAudio.value = it }
        }
    }

    private fun filterAndSortFiles(
        filesList: List<FileItem>,
        query: String,
        category: FileCategory,
        field: SortField,
        ascending: Boolean
    ): List<FileItem> {
        var result = filesList
        if (query.isNotBlank()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }

        val categorized = when (category) {
            FileCategory.ALL -> result
            FileCategory.VIDEOS -> result.filter { it.isDir || it.isVideo }
            FileCategory.AUDIO -> result.filter { it.isDir || it.isAudio }
            FileCategory.IMAGES -> result.filter { it.isDir || it.isImage }
            FileCategory.DOCUMENTS -> result.filter { it.isDir || it.isDoc }
        }

        val comparator = compareByDescending<FileItem> { it.isDir }

        val sorted = when (field) {
            SortField.NAME -> {
                if (ascending) {
                    comparator.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                } else {
                    comparator.thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            }
            SortField.SIZE -> {
                if (ascending) {
                    comparator.thenBy { it.size }
                } else {
                    comparator.thenByDescending { it.size }
                }
            }
            SortField.TYPE -> {
                if (ascending) {
                    comparator.thenBy { it.extension }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                } else {
                    comparator.thenByDescending { it.extension }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            }
        }

        return categorized.sortedWith(sorted)
    }

    companion object {
        fun isVideoFile(name: String): Boolean = FileItem.isVideoFile(name)
        fun isAudioFile(name: String): Boolean = FileItem.isAudioFile(name)
        fun isImageFile(name: String): Boolean = FileItem.isImageFile(name)
        fun isDocFile(name: String): Boolean = FileItem.isDocFile(name)
    }
}

