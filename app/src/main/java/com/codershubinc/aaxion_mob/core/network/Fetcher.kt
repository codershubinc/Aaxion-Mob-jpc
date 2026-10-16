package com.codershubinc.aaxion_mob.core.network


import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Fetcher private constructor() {
    private var apiService: ApiService? = null
    private var currentBaseUrl: String? = null
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
        if (okHttpClient == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
        }
        return okHttpClient!!
    }

    fun getService(baseUrl: String): ApiService {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        if (apiService == null || currentBaseUrl != formattedBaseUrl) {
            currentBaseUrl = formattedBaseUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(formattedBaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}
