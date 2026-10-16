package com.codershubinc.aaxion_mob.core.data

import android.content.Context
import android.util.Log
import com.codershubinc.aaxion_mob.core.model.TokenGenerateResponse

import com.codershubinc.aaxion_mob.core.network.Fetcher
import retrofit2.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenManager private constructor(private val context: Context) {

    private val dataStore = DataStoreManager(context)
    private val refreshMutex = Mutex()
    private var periodicJob: Job? = null

    private val _accessTokenState = MutableStateFlow<String?>(null)
    val accessTokenState: StateFlow<String?> = _accessTokenState.asStateFlow()

    private val _masterTokenState = MutableStateFlow<String?>(null)
    val masterTokenState: StateFlow<String?> = _masterTokenState.asStateFlow()

    private val _lastRefreshTimestamp = MutableStateFlow<Long>(0L)
    val lastRefreshTimestamp: StateFlow<Long> = _lastRefreshTimestamp.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    companion object {
        private const val TAG = "TokenManager"
        const val REFRESH_INTERVAL_MS = 4 * 60 * 60 * 1000L // 4 Hours

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun loadInitialTokens() {
        val master = dataStore.masterToken.firstOrNull()
        val access = dataStore.accessToken.firstOrNull()
        val ts = dataStore.tokenTimestamp.firstOrNull() ?: 0L

        _masterTokenState.value = master
        _accessTokenState.value = access
        _lastRefreshTimestamp.value = ts
        _isInitialized.value = true
    }

    suspend fun saveTokens(
        masterToken: String,
        accessToken: String,
        serverUri: String? = null,
        deviceName: String? = null
    ) {
        val now = System.currentTimeMillis()
        dataStore.saveLoginSession(
            masterToken = masterToken,
            accessToken = accessToken,
            serverUri = serverUri,
            deviceName = deviceName,
            timestamp = now
        )
        _masterTokenState.value = masterToken
        _accessTokenState.value = accessToken
        _lastRefreshTimestamp.value = now
    }

    suspend fun refreshAccessToken(serverUri: String): Boolean = refreshMutex.withLock {
        val master = _masterTokenState.value ?: dataStore.masterToken.firstOrNull()
        if (master.isNullOrBlank()) {
            Log.w(TAG, "Cannot refresh access token: Master token is missing.")
            return false
        }

        _isRefreshing.value = true
        return try {
            Log.d(TAG, "Requesting new access token from $serverUri using master token...")
            val apiService = Fetcher.getInstance().getService(serverUri)
            val response = apiService.generateAccessToken(masterBearerToken = "Bearer $master")

            if (response.isSuccessful && response.body() != null) {
                val newToken = response.body()?.token
                if (!newToken.isNullOrBlank()) {
                    val now = System.currentTimeMillis()
                    dataStore.updateAccessToken(newToken, now)
                    _accessTokenState.value = newToken
                    _lastRefreshTimestamp.value = now
                    Log.i(TAG, "Access token successfully refreshed!")
                    true
                } else {
                    Log.e(TAG, "Empty token received in response body.")
                    false
                }
            } else {
                Log.e(TAG, "Token refresh failed with HTTP ${response.code()}: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            false
        } finally {
            _isRefreshing.value = false
        }
    }

    fun startPeriodicRefresh(scope: CoroutineScope, serverUriProvider: () -> String) {
        periodicJob?.cancel()
        periodicJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting periodic 4-hour token refresher loop.")
            while (isActive) {
                val uri = serverUriProvider()
                val master = _masterTokenState.value ?: dataStore.masterToken.firstOrNull()
                val lastTs = _lastRefreshTimestamp.value

                if (!uri.isNullOrBlank() && !master.isNullOrBlank()) {
                    val elapsed = System.currentTimeMillis() - lastTs
                    if (lastTs > 0 && elapsed >= REFRESH_INTERVAL_MS) {
                        Log.i(TAG, "4 hours elapsed since last token generation. Refreshing access token now...")
                        refreshAccessToken(uri)
                    }
                }

                // Check every 10 minutes to verify if 4 hours have passed
                delay(10 * 60 * 1000L)
            }
        }
    }

    /**
     * Checks if the access token has expired or is nearing expiration (older than 3.5 hours)
     * or is completely missing, and regenerates a fresh one if necessary using master token.
     */
    suspend fun getValidAccessToken(serverUri: String): String? {
        val currentAccess = _accessTokenState.value ?: dataStore.accessToken.firstOrNull()
        val lastTs = _lastRefreshTimestamp.value
        val elapsed = System.currentTimeMillis() - lastTs

        // If access token is missing or older than 3.5 hours (refresh buffer before 4h expiry), refresh it
        val isExpiredOrNearing = currentAccess.isNullOrBlank() || lastTs <= 0L || elapsed >= (3.5 * 60 * 60 * 1000L)
        if (isExpiredOrNearing) {
            Log.d(TAG, "Access token expired or near expiration (elapsed: ${elapsed}ms). Refreshing...")
            val success = refreshAccessToken(serverUri)
            if (success) {
                return _accessTokenState.value
            }
        }
        return currentAccess
    }

    /**
     * Forcefully re-validates the token when an HTTP 401 Unauthorized occurs,
     * re-generates an access token using the master token, and returns the newly generated token.
     */
    suspend fun handleUnauthorizedAndRegenerate(serverUri: String): String? {
        Log.w(TAG, "Received 401 Unauthorized. Access token is invalid or expired. Regenerating...")
        val success = refreshAccessToken(serverUri)
        return if (success) _accessTokenState.value else null
    }

    /**
     * Executes a network call with the currently valid access token.
     * If the server responds with HTTP 401 Unauthorized, automatically regenerates the token
     * and retries the call once.
     */
    suspend fun <T> withAuthRetry(
        serverUri: String,
        fallbackToken: String = "",
        call: suspend (token: String) -> Response<T>
    ): Response<T> {
        val initialToken = getValidAccessToken(serverUri)?.ifBlank { null } ?: fallbackToken
        var response = call(initialToken)
        if (response.code() == 401) {
            val refreshed = handleUnauthorizedAndRegenerate(serverUri)
            if (!refreshed.isNullOrBlank()) {
                response = call(refreshed)
            }
        }
        return response
    }

    suspend fun clearSession() {
        periodicJob?.cancel()
        dataStore.clearAll()
        _masterTokenState.value = null
        _accessTokenState.value = null
        _lastRefreshTimestamp.value = 0L
    }
}
