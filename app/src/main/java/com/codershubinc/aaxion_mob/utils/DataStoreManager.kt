package com.codershubinc.aaxion_mob.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val SERVER_IP_KEY = stringPreferencesKey("server_ip")
        val SERVER_PORT_KEY = stringPreferencesKey("server_port")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    val token: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[TOKEN_KEY]
        }

    suspend fun saveServerInfo(ip: String, port: Int) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_IP_KEY] = ip
            preferences[SERVER_PORT_KEY] = port.toString()
        }
    }

    val serverIp: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_IP_KEY]
        }

    val serverPort: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_PORT_KEY]
        }

    suspend fun saveString(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    fun getString(key: String): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
