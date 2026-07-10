package com.codershubinc.aaxion_mob.utils

import com.codershubinc.aaxion_mob.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Fetcher private constructor() {
    private var apiService: ApiService? = null
    private var currentBaseUrl: String? = null

    companion object {
        private var instance: Fetcher? = null

        fun getInstance(): Fetcher {
            if (instance == null) {
                instance = Fetcher()
            }
            return instance!!
        }
    }

    fun getService(baseUrl: String): ApiService {
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        if (apiService == null || currentBaseUrl != formattedBaseUrl) {
            currentBaseUrl = formattedBaseUrl
            
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
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
