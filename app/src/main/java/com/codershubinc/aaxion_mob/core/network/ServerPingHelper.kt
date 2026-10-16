package com.codershubinc.aaxion_mob.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Reusable helper for server latency pings and health checks.
 */
object ServerPingHelper {

    private val fastPingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .writeTimeout(1500, TimeUnit.MILLISECONDS)
            .callTimeout(2000, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    suspend fun pingServer(
        client: OkHttpClient = fastPingClient,
        uri: String
    ): Long? {
        if (uri.isBlank()) return null
        val cleanUri = if (uri.endsWith("/")) uri.dropLast(1) else uri
        val pingUrl = "$cleanUri/api/v1/system/storage"
        return try {
            val request = Request.Builder().url(pingUrl).head().build()
            val startTime = System.currentTimeMillis()

            suspendCancellableCoroutine { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation {
                    try {
                        call.cancel()
                    } catch (_: Throwable) {}
                }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use { res ->
                            val elapsed = System.currentTimeMillis() - startTime
                            if (continuation.isActive) {
                                if (res.isSuccessful || res.code in 200..499) {
                                    continuation.resume(elapsed)
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            null
        }
    }
}

