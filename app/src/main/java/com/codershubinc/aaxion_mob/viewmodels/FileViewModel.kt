package com.codershubinc.aaxion_mob.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.utils.Fetcher
import kotlinx.coroutines.launch

class FileViewModel : ViewModel() {
    var files by mutableStateOf<List<FileItem>>(emptyList())
        private set
    
    var currentPath by mutableStateOf("/")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun fetchFiles(baseUrl: String, token: String, path: String = "/") {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val apiService = Fetcher.getInstance().getService(baseUrl)
                val response = apiService.listFiles(path, "Bearer $token")
                if (response.isSuccessful) {
                    files = response.body() ?: emptyList()
                    currentPath = path
                } else {
                    errorMessage = "Error: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }
}
