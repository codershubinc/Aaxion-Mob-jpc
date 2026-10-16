package com.codershubinc.aaxion_mob.core.model

import com.google.gson.annotations.SerializedName

data class DeviceInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null
)

data class LoginResponse(
    @SerializedName("token") val masterToken: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("device_info") val deviceInfo: DeviceInfo? = null
)

data class TokenGenerateResponse(
    @SerializedName("token") val token: String? = null
)
