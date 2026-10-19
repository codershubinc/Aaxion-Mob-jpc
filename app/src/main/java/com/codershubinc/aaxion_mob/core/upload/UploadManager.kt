package com.codershubinc.aaxion_mob.core.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.network.AaxionUrlBuilder
import com.codershubinc.aaxion_mob.core.network.ApiEndpoints
import com.codershubinc.aaxion_mob.core.network.CountingRequestBody
import com.codershubinc.aaxion_mob.core.network.Fetcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

/**
 * Centralized upload orchestrator for managing file uploads in the background.
 * Uses direct streaming upload for local IP connections, and 3-step chunked uploads for tunnel URLs.
 */
class UploadManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _tasks = MutableStateFlow<List<UploadTask>>(emptyList())
    val tasks: StateFlow<List<UploadTask>> = _tasks.asStateFlow()

    private val _hasActiveUploads = MutableStateFlow(false)
    val hasActiveUploads: StateFlow<Boolean> = _hasActiveUploads.asStateFlow()

    private val _activeSpeed = MutableStateFlow(0L)
    val activeSpeed: StateFlow<Long> = _activeSpeed.asStateFlow()

    private val _uploadCompletedEvents = MutableSharedFlow<UploadTask>(extraBufferCapacity = 32)
    val uploadCompletedEvents: SharedFlow<UploadTask> = _uploadCompletedEvents.asSharedFlow()

    private val cancelledTaskIds = mutableSetOf<String>()
    private var workerJob: Job? = null

    companion object {
        @Volatile
        private var instance: UploadManager? = null

        fun getInstance(context: Context): UploadManager {
            return instance ?: synchronized(this) {
                instance ?: UploadManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isLocalServerUri(uriStr: String): Boolean {
        return try {
            val host = Uri.parse(uriStr).host ?: uriStr
            host.equals("localhost", ignoreCase = true) ||
                    host == "127.0.0.1" ||
                    host.startsWith("192.168.") ||
                    host.startsWith("10.") ||
                    (host.startsWith("172.") && host.split(".").let { parts ->
                        parts.size >= 2 && (parts[1].toIntOrNull() ?: 0) in 16..31
                    })
        } catch (_: Exception) {
            false
        }
    }

    fun enqueueUploads(
        context: Context,
        serverUri: String,
        token: String,
        targetDir: String,
        uris: List<Uri>
    ) {
        if (uris.isEmpty() || serverUri.isBlank()) return

        val newTasks = uris.mapNotNull { uri ->
            resolveFileMetadata(context, uri)?.let { (name, size) ->
                UploadTask(
                    name = name,
                    uri = uri,
                    sizeBytes = size,
                    targetDir = targetDir,
                    serverUri = serverUri,
                    token = token,
                    status = UploadStatus.QUEUED
                )
            }
        }

        if (newTasks.isEmpty()) return

        _tasks.update { it + newTasks }
        startServiceIfRunning(context)
        triggerWorker()
    }

    fun cancelUpload(taskId: String) {
        cancelledTaskIds.add(taskId)
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    task.copy(status = UploadStatus.CANCELLED, speedBytesPerSec = 0L)
                } else task
            }
        }
        checkActiveState()
    }

    fun cancelAll() {
        val currentIds = _tasks.value.map { it.id }
        cancelledTaskIds.addAll(currentIds)
        _tasks.update { list ->
            list.map { task ->
                if (task.status == UploadStatus.QUEUED || task.status == UploadStatus.UPLOADING) {
                    task.copy(status = UploadStatus.CANCELLED, speedBytesPerSec = 0L)
                } else task
            }
        }
        checkActiveState()
    }

    fun clearFinished() {
        _tasks.update { list ->
            list.filter { it.status == UploadStatus.QUEUED || it.status == UploadStatus.UPLOADING }
        }
        checkActiveState()
    }

    fun retryUpload(context: Context, taskId: String) {
        cancelledTaskIds.remove(taskId)
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    task.copy(
                        status = UploadStatus.QUEUED,
                        bytesUploaded = 0L,
                        speedBytesPerSec = 0L,
                        errorMessage = null
                    )
                } else task
            }
        }
        startServiceIfRunning(context)
        triggerWorker()
    }

    private fun startServiceIfRunning(context: Context) {
        val serviceIntent = Intent(context, FileUploadService::class.java).apply {
            action = FileUploadService.ACTION_START_UPLOAD
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {
            // fallback if service cannot start immediately
        }
    }

    private fun checkActiveState() {
        val active = _tasks.value.any { it.status == UploadStatus.QUEUED || it.status == UploadStatus.UPLOADING }
        _hasActiveUploads.value = active
        if (!active) {
            _activeSpeed.value = 0L
        }
    }

    private fun triggerWorker() {
        if (workerJob?.isActive == true) return
        workerJob = scope.launch {
            while (isActive) {
                val nextTask = _tasks.value.firstOrNull { it.status == UploadStatus.QUEUED }
                if (nextTask == null) {
                    checkActiveState()
                    break
                }
                _hasActiveUploads.value = true
                executeSingleUpload(nextTask)
            }
            checkActiveState()
        }
    }

    private suspend fun executeSingleUpload(task: UploadTask) {
        if (cancelledTaskIds.contains(task.id)) {
            updateTaskStatus(task.id, UploadStatus.CANCELLED)
            return
        }

        updateTaskStatus(task.id, UploadStatus.UPLOADING)

        var effectiveToken = task.token
        val tokenManager = TokenManager.getInstance(appContext)
        val activeAccessToken = tokenManager.accessTokenState.value
        if (!activeAccessToken.isNullOrBlank()) {
            effectiveToken = activeAccessToken
        }

        val success = uploadWithRouting(task, effectiveToken)
        if (!success && !cancelledTaskIds.contains(task.id)) {
            // Attempt token refresh once if unauthorized
            val refreshed = tokenManager.refreshAccessToken(task.serverUri)
            if (refreshed) {
                val newTok = tokenManager.accessTokenState.value ?: effectiveToken
                uploadWithRouting(task, newTok)
            }
        }
    }

    private suspend fun uploadWithRouting(task: UploadTask, token: String): Boolean {
        val isLocal = isLocalServerUri(task.serverUri)
        return if (isLocal) {
            uploadDirect(task, token)
        } else {
            uploadChunked(task, token)
        }
    }

    /**
     * Direct streaming multipart upload for local network IPs.
     */
    private suspend fun uploadDirect(task: UploadTask, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = appContext.contentResolver
            val targetUri = task.uri ?: throw IOException("URI is null for: ${task.name}")
            val inputStream = contentResolver.openInputStream(targetUri)
                ?: throw IOException("Cannot open input stream for: ${task.name}")

            val mimeType = contentResolver.getType(targetUri) ?: "application/octet-stream"

            val streamBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaTypeOrNull()

                override fun contentLength(): Long = task.sizeBytes

                override fun writeTo(sink: BufferedSink) {
                    inputStream.use { input ->
                        input.source().use { source ->
                            sink.writeAll(source)
                        }
                    }
                }
            }

            val countingBody = CountingRequestBody(
                delegate = streamBody,
                onProgress = { bytesWritten, totalBytes, speedBytesPerSec ->
                    _activeSpeed.value = speedBytesPerSec
                    _tasks.update { list ->
                        list.map { item ->
                            if (item.id == task.id) {
                                item.copy(
                                    bytesUploaded = bytesWritten,
                                    sizeBytes = if (totalBytes > 0) totalBytes else item.sizeBytes,
                                    speedBytesPerSec = speedBytesPerSec
                                )
                            } else item
                        }
                    }
                },
                isCancelled = { cancelledTaskIds.contains(task.id) }
            )

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", task.name, countingBody)
                .build()

            val encodedDir = AaxionUrlBuilder.encodePath(task.targetDir)
            val uploadUrl = "${ApiEndpoints.url(task.serverUri, ApiEndpoints.FILES.UPLOAD)}?dir=$encodedDir"

            val request = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            val client = Fetcher.getInstance().getClient()
            client.newCall(request).execute().use { response ->
                if (cancelledTaskIds.contains(task.id)) {
                    updateTaskStatus(task.id, UploadStatus.CANCELLED)
                    return@withContext false
                }

                if (response.isSuccessful || response.code in 200..201) {
                    val finishedTask = task.copy(
                        status = UploadStatus.SUCCESS,
                        bytesUploaded = task.sizeBytes,
                        speedBytesPerSec = 0L
                    )
                    _tasks.update { list ->
                        list.map { item ->
                            if (item.id == task.id) finishedTask else item
                        }
                    }
                    _uploadCompletedEvents.tryEmit(finishedTask)
                    true
                } else {
                    val errMsg = "HTTP ${response.code}: ${response.message.ifBlank { "Upload failed" }}"
                    _tasks.update { list ->
                        list.map { item ->
                            if (item.id == task.id) {
                                item.copy(status = UploadStatus.FAILED, errorMessage = errMsg, speedBytesPerSec = 0L)
                            } else item
                        }
                    }
                    false
                }
            }
        } catch (e: Exception) {
            val status = if (cancelledTaskIds.contains(task.id)) UploadStatus.CANCELLED else UploadStatus.FAILED
            val errMsg = if (status == UploadStatus.CANCELLED) null else (e.localizedMessage ?: "Upload error")
            _tasks.update { list ->
                list.map { item ->
                    if (item.id == task.id) {
                        item.copy(status = status, errorMessage = errMsg, speedBytesPerSec = 0L)
                    } else item
                }
            }
            false
        }
    }

    /**
     * Chunked upload flow for tunnel URLs (keeps chunks under 15MB each to easily satisfy tunnel/proxy limits).
     */
    private suspend fun uploadChunked(task: UploadTask, token: String): Boolean = withContext(Dispatchers.IO) {
        val client = Fetcher.getInstance().getClient()
        val chunkSize = 10 * 1024 * 1024L // 10MB chunk size
        val encodedFilename = AaxionUrlBuilder.encodePath(task.name)

        try {
            // Step 1: Start chunked upload session
            val startUrl = "${ApiEndpoints.url(task.serverUri, ApiEndpoints.CHUNKS.START)}?filename=$encodedFilename"
            val startRequest = Request.Builder()
                .url(startUrl)
                .header("Authorization", "Bearer $token")
                .post(ByteArray(0).toRequestBody(null))
                .build()

            client.newCall(startRequest).execute().use { response ->
                if (!response.isSuccessful && response.code !in 200..201) {
                    val errMsg = "Chunk init failed: HTTP ${response.code}"
                    _tasks.update { list ->
                        list.map { item -> if (item.id == task.id) item.copy(status = UploadStatus.FAILED, errorMessage = errMsg) else item }
                    }
                    return@withContext false
                }
            }

            if (cancelledTaskIds.contains(task.id)) {
                updateTaskStatus(task.id, UploadStatus.CANCELLED)
                return@withContext false
            }

            // Step 2: Upload chunks
            val contentResolver = appContext.contentResolver
            val targetUri = task.uri ?: throw IOException("URI is null for: ${task.name}")
            val inputStream = contentResolver.openInputStream(targetUri)
                ?: throw IOException("Cannot open input stream for: ${task.name}")

            var chunkIndex = 0
            var totalBytesUploaded = 0L
            val buffer = ByteArray(chunkSize.toInt())

            inputStream.use { stream ->
                while (true) {
                    if (cancelledTaskIds.contains(task.id)) {
                        updateTaskStatus(task.id, UploadStatus.CANCELLED)
                        return@withContext false
                    }

                    var bytesReadThisChunk = 0
                    while (bytesReadThisChunk < chunkSize) {
                        val read = stream.read(buffer, bytesReadThisChunk, (chunkSize - bytesReadThisChunk).toInt())
                        if (read == -1) break
                        bytesReadThisChunk += read
                    }

                    if (bytesReadThisChunk == 0) {
                        // Empty file support (ensure chunk 0 was sent)
                        if (chunkIndex == 0) {
                            val uploadChunkUrl = "${ApiEndpoints.url(task.serverUri, ApiEndpoints.CHUNKS.UPLOAD)}?filename=$encodedFilename&chunk_index=0"
                            val emptyBody = ByteArray(0).toRequestBody("application/octet-stream".toMediaTypeOrNull())
                            val req = Request.Builder()
                                .url(uploadChunkUrl)
                                .header("Authorization", "Bearer $token")
                                .post(emptyBody)
                                .build()
                            client.newCall(req).execute().close()
                        }
                        break
                    }

                    val chunkBytes = if (bytesReadThisChunk == buffer.size) {
                        buffer
                    } else {
                        buffer.copyOf(bytesReadThisChunk)
                    }

                    val rawBody = chunkBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val countingBody = CountingRequestBody(
                        delegate = rawBody,
                        onProgress = { written, _, speed ->
                            _activeSpeed.value = speed
                            val liveUploaded = totalBytesUploaded + written
                            _tasks.update { list ->
                                list.map { item ->
                                    if (item.id == task.id) {
                                        item.copy(
                                            bytesUploaded = liveUploaded,
                                            speedBytesPerSec = speed
                                        )
                                    } else item
                                }
                            }
                        },
                        isCancelled = { cancelledTaskIds.contains(task.id) }
                    )

                    val uploadChunkUrl = "${ApiEndpoints.url(task.serverUri, ApiEndpoints.CHUNKS.UPLOAD)}?filename=$encodedFilename&chunk_index=$chunkIndex"
                    val chunkReq = Request.Builder()
                        .url(uploadChunkUrl)
                        .header("Authorization", "Bearer $token")
                        .post(countingBody)
                        .build()

                    client.newCall(chunkReq).execute().use { chunkRes ->
                        if (!chunkRes.isSuccessful && chunkRes.code !in 200..201) {
                            val errMsg = "Chunk $chunkIndex failed: HTTP ${chunkRes.code}"
                            _tasks.update { list ->
                                list.map { item -> if (item.id == task.id) item.copy(status = UploadStatus.FAILED, errorMessage = errMsg) else item }
                            }
                            return@withContext false
                        }
                    }

                    totalBytesUploaded += bytesReadThisChunk
                    chunkIndex++
                }
            }

            // Step 3: Complete & Merge Chunks
            val encodedDir = AaxionUrlBuilder.encodePath(task.targetDir)
            val completeUrl = "${ApiEndpoints.url(task.serverUri, ApiEndpoints.CHUNKS.COMPLETE)}?filename=$encodedFilename&dir=$encodedDir"
            val completeRequest = Request.Builder()
                .url(completeUrl)
                .header("Authorization", "Bearer $token")
                .post(ByteArray(0).toRequestBody(null))
                .build()

            client.newCall(completeRequest).execute().use { response ->
                if (response.isSuccessful || response.code in 200..201) {
                    val finishedTask = task.copy(
                        status = UploadStatus.SUCCESS,
                        bytesUploaded = task.sizeBytes,
                        speedBytesPerSec = 0L
                    )
                    _tasks.update { list ->
                        list.map { item ->
                            if (item.id == task.id) finishedTask else item
                        }
                    }
                    _uploadCompletedEvents.tryEmit(finishedTask)
                    true
                } else {
                    val errMsg = "Chunk complete failed: HTTP ${response.code}"
                    _tasks.update { list ->
                        list.map { item -> if (item.id == task.id) item.copy(status = UploadStatus.FAILED, errorMessage = errMsg) else item }
                    }
                    false
                }
            }
        } catch (e: Exception) {
            val status = if (cancelledTaskIds.contains(task.id)) UploadStatus.CANCELLED else UploadStatus.FAILED
            val errMsg = if (status == UploadStatus.CANCELLED) null else (e.localizedMessage ?: "Chunked upload error")
            _tasks.update { list ->
                list.map { item -> if (item.id == task.id) item.copy(status = status, errorMessage = errMsg) else item }
            }
            false
        }
    }

    private fun updateTaskStatus(taskId: String, status: UploadStatus) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) {
                    task.copy(status = status, speedBytesPerSec = if (status == UploadStatus.UPLOADING) task.speedBytesPerSec else 0L)
                } else task
            }
        }
    }

    private fun resolveFileMetadata(context: Context, uri: Uri): Pair<String, Long>? {
        return try {
            var name = uri.lastPathSegment ?: "uploaded_file"
            var size = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val n = cursor.getString(nameIndex)
                        if (!n.isNullOrBlank()) name = n
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
            Pair(name, size)
        } catch (e: Exception) {
            Pair(uri.lastPathSegment ?: "file", 0L)
        }
    }
}
