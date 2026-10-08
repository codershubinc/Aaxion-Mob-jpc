package com.codershubinc.aaxion_mob.auth

import android.content.Context
import com.codershubinc.aaxion_mob.utils.Fetcher

data class AuthResult(
    val success: Boolean,
    val masterToken: String? = null,
    val accessToken: String? = null,
    val deviceName: String? = null,
    val errorMessage: String? = null
)

class Auth {
    suspend fun login(
        username: String,
        password: String,
        serverUri: String,
        context: Context? = null
    ): AuthResult {
        return try {
            val apiService = Fetcher.getInstance().getService(serverUri)
            val response = apiService.login(
                mapOf("password" to password, "username" to username)
            )

            if (response.isSuccessful) {
                val body = response.body()
                val masterToken = body?.masterToken
                val accessToken = body?.accessToken
                val deviceName = body?.deviceInfo?.name

                if (!masterToken.isNullOrBlank() && !accessToken.isNullOrBlank()) {
                    if (context != null) {
                        TokenManager.getInstance(context).saveTokens(
                            masterToken = masterToken,
                            accessToken = accessToken,
                            serverUri = serverUri,
                            deviceName = deviceName
                        )
                    }
                    AuthResult(
                        success = true,
                        masterToken = masterToken,
                        accessToken = accessToken,
                        deviceName = deviceName
                    )
                } else if (!masterToken.isNullOrBlank()) {
                    // Fallback if accessToken wasn't provided directly
                    if (context != null) {
                        TokenManager.getInstance(context).saveTokens(
                            masterToken = masterToken,
                            accessToken = masterToken,
                            serverUri = serverUri,
                            deviceName = deviceName
                        )
                    }
                    AuthResult(
                        success = true,
                        masterToken = masterToken,
                        accessToken = masterToken,
                        deviceName = deviceName
                    )
                } else {
                    AuthResult(
                        success = false,
                        errorMessage = "Login response missing authentication tokens"
                    )
                }
            } else {
                val errorBody = response.errorBody()?.string()
                AuthResult(
                    success = false,
                    errorMessage = errorBody ?: "HTTP ${response.code()}: Login failed"
                )
            }
        } catch (e: Exception) {
            AuthResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Network connection error"
            )
        }
    }
}
