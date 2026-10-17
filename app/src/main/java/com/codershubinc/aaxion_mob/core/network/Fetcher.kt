package com.codershubinc.aaxion_mob.core.network


import okhttp3.OkHttpClient
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
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
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
