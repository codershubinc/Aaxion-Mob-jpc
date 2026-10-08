package com.codershubinc.aaxion_mob.api

import com.codershubinc.aaxion_mob.constants.API_ENDPOINTS
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.models.LoginResponse
import com.codershubinc.aaxion_mob.models.RootPathResponse
import com.codershubinc.aaxion_mob.models.StorageInfoResponse
import com.codershubinc.aaxion_mob.models.TokenGenerateResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST(API_ENDPOINTS.AUTH.LOGIN)
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @POST(API_ENDPOINTS.AUTH.TOKEN_GENERATE)
    suspend fun generateAccessToken(
        @Header("Authorization") masterBearerToken: String
    ): Response<TokenGenerateResponse>

    @POST(API_ENDPOINTS.AUTH.LOGOUT)
    suspend fun logout(
        @Header("Authorization") bearerToken: String
    ): Response<Void>

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
