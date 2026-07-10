package com.codershubinc.aaxion_mob.api

import com.codershubinc.aaxion_mob.constants.API_ENDPOINTS
import com.codershubinc.aaxion_mob.models.LoginResponse
import com.codershubinc.aaxion_mob.models.FileItem
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST(API_ENDPOINTS.AUTH.LOGIN)
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @GET(API_ENDPOINTS.FILES.VIEW)
    suspend fun listFiles(
        @Query("dir") path: String,
        @Header("Authorization") token: String
    ): Response<List<FileItem>>
 
}
