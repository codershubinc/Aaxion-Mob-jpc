with open("app/src/main/java/com/codershubinc/aaxion_mob/utils/DataStoreManager.kt", "r") as f:
    content = f.read()

content = content.replace(
    'val PRIORITIZED_SERVER_URI_KEY = stringPreferencesKey("prioritized_server_uri")',
    'val PRIORITIZED_SERVER_URI_KEY = stringPreferencesKey("prioritized_server_uri")\n        val TUNNEL_SERVER_URI_KEY = stringPreferencesKey("tunnel_server_uri")'
)

new_methods = """
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
"""

# Insert before suspend fun clearAll()
content = content.replace(
    "    suspend fun clearAll() {",
    new_methods + "\n    suspend fun clearAll() {"
)

with open("app/src/main/java/com/codershubinc/aaxion_mob/utils/DataStoreManager.kt", "w") as f:
    f.write(content)
