package com.codershubinc.aaxion_mob.models

import com.google.gson.annotations.SerializedName

data class FileItem(
    @SerializedName("name") val name: String,
    @SerializedName("is_dir") val isDir: Boolean,
    @SerializedName("size") val size: Long,
    @SerializedName("path") val path: String,
    @SerializedName("raw_path") val rawPath: String
)
