package com.codershubinc.aaxion_mob.auth

import com.codershubinc.aaxion_mob.utils.Fetcher


class Auth {
    suspend fun login (
        username: String,
        password: String,
        serverUri: String
    ): Pair<String, Boolean> {
        return try {
            val apiService = Fetcher.getInstance().getService(serverUri)
            val response = apiService.login(
                mapOf("password" to password, "username" to username)
            )

            if (response.isSuccessful) { 
                val body = response.body()
                val token = body?.token
                if (!token.isNullOrBlank()) {
                    Pair(token, true)
                } else {
                    Pair("Login response missing token", false)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Pair(errorBody ?: "Unknown error", false)
            }
        } catch (e: Exception) {
            Pair(e.message ?: "Network error occurred", false)
        }
    }
}
