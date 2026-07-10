package com.codershubinc.aaxion_mob.models

import com.google.gson.annotations.SerializedName

data class DeviceInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null
)

data class LoginResponse(
    @SerializedName("token") val token: String? = null,
    @SerializedName("device_info") val deviceInfo: DeviceInfo? = null
)

