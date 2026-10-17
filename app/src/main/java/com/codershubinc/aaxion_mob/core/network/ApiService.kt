package com.codershubinc.aaxion_mob.core.network

import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.LoginResponse
import com.codershubinc.aaxion_mob.core.model.RootPathResponse
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.model.TokenGenerateResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST(ApiEndpoints.AUTH.LOGIN)
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @POST(ApiEndpoints.AUTH.TOKEN_GENERATE)
    suspend fun generateAccessToken(
        @Header("Authorization") masterBearerToken: String
    ): Response<TokenGenerateResponse>

    @POST(ApiEndpoints.AUTH.LOGOUT)
    suspend fun logout(
        @Header("Authorization") bearerToken: String
    ): Response<Void>

    @GET(ApiEndpoints.FILES.VIEW)
    suspend fun listFiles(
        @Query("dir") path: String,
        @Header("Authorization") token: String
    ): Response<List<FileItem>>

    @GET(ApiEndpoints.SYSTEM.GET_ROOT_PATH)
    suspend fun getRootPath(
        @Header("Authorization") token: String
    ): Response<RootPathResponse>

    @GET(ApiEndpoints.SYSTEM.STORAGE)
    suspend fun getStorageInfo(
        @Header("Authorization") token: String
    ): Response<StorageInfoResponse>

    // ==========================================
    // Music Endpoints
    // ==========================================
    @GET(ApiEndpoints.MUSIC.LIST)
    suspend fun getMusicTracks(
        @Header("Authorization") token: String
    ): Response<List<com.codershubinc.aaxion_mob.core.model.MusicTrack>>

    @GET(ApiEndpoints.MUSIC.GET)
    suspend fun getMusicTrackById(
        @Query("id") id: Long,
        @Header("Authorization") token: String
    ): Response<com.codershubinc.aaxion_mob.core.model.MusicTrack>

    @GET(ApiEndpoints.MUSIC.SEARCH)
    suspend fun searchMusicTracks(
        @Query("title") title: String,
        @Header("Authorization") token: String
    ): Response<List<com.codershubinc.aaxion_mob.core.model.MusicTrack>>

    @POST(ApiEndpoints.MUSIC.ADD)
    suspend fun addMusicTrack(
        @Query("uri") uri: String,
        @Header("Authorization") token: String
    ): Response<com.codershubinc.aaxion_mob.core.model.AddTrackResponse>

    @PUT(ApiEndpoints.MUSIC.UPDATE)
    suspend fun updateMusicTrack(
        @Body track: com.codershubinc.aaxion_mob.core.model.UpdateTrackRequest,
        @Header("Authorization") token: String
    ): Response<com.codershubinc.aaxion_mob.core.model.MusicTrack>
}
