package com.codershubinc.aaxion_mob.core.network

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
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
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
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
        val pingUrl = ApiEndpoints.pingUrl(uri)
        return try {
            val request = Request.Builder()
                .url(pingUrl)
                .header("Connection", "keep-alive")
                .get()
                .build()
            val startNs = System.nanoTime()

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
                            // Drain body to allow connection reuse in pool
                            try {
                                res.body?.source()?.skip(Long.MAX_VALUE)
                            } catch (_: Throwable) {}
                            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                            if (continuation.isActive) {
                                if (res.isSuccessful || res.code in 200..499) {
                                    continuation.resume(elapsedMs.coerceAtLeast(1L))
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

    /**
     * Executes ping request against /api/v1/ping and parses the PingResponse payload.
     */
    suspend fun fetchPing(
        client: OkHttpClient = fastPingClient,
        uri: String
    ): Pair<Long, com.codershubinc.aaxion_mob.core.model.PingResponse>? {
        if (uri.isBlank()) return null
        val pingUrl = ApiEndpoints.pingUrl(uri)
        return try {
            val request = Request.Builder()
                .url(pingUrl)
                .header("Connection", "keep-alive")
                .get()
                .build()
            val startNs = System.nanoTime()

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
                            val bodyStr = try {
                                res.body?.string() ?: ""
                            } catch (_: Throwable) {
                                ""
                            }
                            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                            if (continuation.isActive) {
                                if (res.isSuccessful && bodyStr.isNotBlank()) {
                                    val parsed = com.google.gson.Gson().fromJson(
                                        bodyStr,
                                        com.codershubinc.aaxion_mob.core.model.PingResponse::class.java
                                    )
                                    continuation.resume(Pair(elapsedMs.coerceAtLeast(1L), parsed))
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

