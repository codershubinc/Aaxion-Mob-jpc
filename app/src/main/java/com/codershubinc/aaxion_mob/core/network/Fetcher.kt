package com.codershubinc.aaxion_mob.core.network


import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Fetcher private constructor() {
    @Volatile
    private var apiService: ApiService? = null
    @Volatile
    private var currentBaseUrl: String? = null
    @Volatile
    private var okHttpClient: OkHttpClient? = null

    companion object {
        @Volatile
        private var instance: Fetcher? = null

        fun getInstance(): Fetcher {
            return instance ?: synchronized(this) {
                instance ?: Fetcher().also { instance = it }
            }
        }
    }

    fun getClient(): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: run {
                val logging = SelectiveHttpLoggingInterceptor()
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .build()
                    .also { okHttpClient = it }
            }
        }
    }

    fun getService(baseUrl: String): ApiService {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (apiService == null || currentBaseUrl != formattedBaseUrl) {
            synchronized(this) {
                if (apiService == null || currentBaseUrl != formattedBaseUrl) {
                    currentBaseUrl = formattedBaseUrl

                    val retrofit = Retrofit.Builder()
                        .baseUrl(formattedBaseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(getClient())
                        .build()

                    apiService = retrofit.create(ApiService::class.java)
                }
            }
        }
        return apiService!!
    }
}

/**
 * Selective HTTP logging interceptor that logs full bodies for JSON API requests/responses,
 * but omits file binary bodies for uploads, chunk streaming, and file downloads.
 */
class SelectiveHttpLoggingInterceptor : Interceptor {
    private val bodyLogger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val headersLogger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath.lowercase()
        val contentType = request.body?.contentType()?.toString()?.lowercase() ?: ""

        val isFileUpload = path.contains("/upload") ||
                path.contains("/chunk") ||
                contentType.contains("multipart") ||
                contentType.contains("octet-stream")

        val isBinaryDownload = path.contains("/download") ||
                path.contains("/stream") ||
                path.contains("/thumb") ||
                path.contains("/images/")

        val logger = if (isFileUpload || isBinaryDownload) {
            headersLogger
        } else {
            bodyLogger
        }
        return logger.intercept(chain)
    }
}
