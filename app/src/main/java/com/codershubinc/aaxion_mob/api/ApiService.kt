package com.codershubinc.aaxion_mob.api

import com.codershubinc.aaxion_mob.constants.API_ENDPOINTS
import com.codershubinc.aaxion_mob.models.LoginResponse
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.models.RootPathResponse
import com.codershubinc.aaxion_mob.models.StorageInfoResponse
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

    @GET(API_ENDPOINTS.SYSTEM.GET_ROOT_PATH)
    suspend fun getRootPath(
        @Header("Authorization") token: String
    ): Response<RootPathResponse>

    @GET(API_ENDPOINTS.SYSTEM.STORAGE)
    suspend fun getStorageInfo(
        @Header("Authorization") token: String
    ): Response<StorageInfoResponse>
 
}
