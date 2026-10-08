package com.codershubinc.aaxion_mob.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aaxion_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val MASTER_TOKEN_KEY = stringPreferencesKey("master_token")
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val TOKEN_TIMESTAMP_KEY = longPreferencesKey("token_timestamp")
        val SERVER_IP_KEY = stringPreferencesKey("server_ip")
        val SERVER_PORT_KEY = stringPreferencesKey("server_port")
        val SERVER_URI_KEY = stringPreferencesKey("server_uri")
        val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
        val ACCENT_THEME_KEY = stringPreferencesKey("accent_theme")
        val ICON_PACK_KEY = stringPreferencesKey("icon_pack_theme")
        val PRIORITIZED_SERVER_URI_KEY = stringPreferencesKey("prioritized_server_uri")
        val TUNNEL_SERVER_URI_KEY = stringPreferencesKey("tunnel_server_uri")
    }

    suspend fun setPrioritizedServerUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(PRIORITIZED_SERVER_URI_KEY)
            } else {
                preferences[PRIORITIZED_SERVER_URI_KEY] = uri
                preferences[SERVER_URI_KEY] = uri
            }
        }
    }

    val prioritizedServerUri: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PRIORITIZED_SERVER_URI_KEY] }

    suspend fun setAccentTheme(themeId: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_THEME_KEY] = themeId
        }
    }

    suspend fun setIconPack(packId: String) {
        context.dataStore.edit { preferences ->
            preferences[ICON_PACK_KEY] = packId
        }
    }

    val accentTheme: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[ACCENT_THEME_KEY] }

    val iconPackTheme: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[ICON_PACK_KEY] }

    suspend fun saveLoginSession(
        masterToken: String,
        accessToken: String,
        serverUri: String? = null,
        deviceName: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        context.dataStore.edit { preferences ->
            preferences[MASTER_TOKEN_KEY] = masterToken
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[TOKEN_TIMESTAMP_KEY] = timestamp
            if (!serverUri.isNullOrBlank()) {
                preferences[SERVER_URI_KEY] = serverUri
            }
            if (!deviceName.isNullOrBlank()) {
                preferences[DEVICE_NAME_KEY] = deviceName
            }
        }
    }

    suspend fun updateAccessToken(
        accessToken: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[TOKEN_TIMESTAMP_KEY] = timestamp
        }
    }

    val masterToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[MASTER_TOKEN_KEY] }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[ACCESS_TOKEN_KEY] }

    val tokenTimestamp: Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[TOKEN_TIMESTAMP_KEY] }

    val serverUri: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[SERVER_URI_KEY] }

    val deviceName: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[DEVICE_NAME_KEY] }

    suspend fun saveServerInfo(ip: String, port: Int) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_IP_KEY] = ip
            preferences[SERVER_PORT_KEY] = port.toString()
            preferences[SERVER_URI_KEY] = "http://$ip:$port"
        }
    }


    suspend fun setTunnelServerUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(TUNNEL_SERVER_URI_KEY)
            } else {
                preferences[TUNNEL_SERVER_URI_KEY] = uri
            }
        }
    }

    val tunnelServerUri: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[TUNNEL_SERVER_URI_KEY] }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
