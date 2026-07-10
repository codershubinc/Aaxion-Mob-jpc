package com.codershubinc.aaxion_mob.models

import com.google.gson.annotations.SerializedName

data class RootPathResponse(
    @SerializedName("root_path")
    val rootPath: String
)

data class StorageInfoResponse(
    @SerializedName("total")
    val total: Long,
    @SerializedName("used")
    val used: Long,
    @SerializedName("available")
    val available: Long,
    @SerializedName("usage_percentage")
    val usagePercentage: Double,
    @SerializedName("external_devices")
    val externalDevices: List<ExternalDeviceInfo> = emptyList()
)

data class ExternalDeviceInfo(
    @SerializedName("device")
    val device: String,
    @SerializedName("mount_point")
    val mountPoint: String,
    @SerializedName("filesystem_type")
    val filesystemType: String,
    @SerializedName("total")
    val total: Long,
    @SerializedName("used")
    val used: Long,
    @SerializedName("available")
    val available: Long,
    @SerializedName("usage_percentage")
    val usagePercentage: Double
)
