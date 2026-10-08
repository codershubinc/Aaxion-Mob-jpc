import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "r") as f:
    content = f.read()

# 1. Update signature
sig_old = """fun AaxionDriveScreen(
    serverUri: String,"""
sig_new = """fun AaxionDriveScreen(
    serverUri: String,
    tunnelUri: String? = null,"""
content = content.replace(sig_old, sig_new)

# 2. Add isConnected, isLocal parameters to DriveFilesView? No, just pass them to DriveFilesView.
# Wait, let's update the ping loop first.
ping_old = """    var serverLatencyMs by remember { mutableStateOf<Long?>(null) }
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(serverUri) {
        if (serverUri.isNotBlank()) {
            while (isActive) {
                try {
                    val client = Fetcher.getInstance().getClient()
                    val pingUrl = if (serverUri.endsWith("/")) "${serverUri}api/v1/system/storage" else "$serverUri/api/v1/system/storage"
                    val request = okhttp3.Request.Builder()
                        .url(pingUrl)
                        .head()
                        .build()

                    val startTime = System.currentTimeMillis()
                    val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        try {
                            client.newCall(request).execute()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    if (response != null && (response.isSuccessful || response.code in 200..499)) {
                        response.close()
                        serverLatencyMs = elapsed
                        isServerOnline = true
                    } else {
                        serverLatencyMs = null
                        isServerOnline = false
                    }
                } catch (e: Exception) {
                    serverLatencyMs = null
                    isServerOnline = false
                }
                delay(8000L) // Ping every 8 seconds
            }
        } else {
            isServerOnline = false
            serverLatencyMs = null
        }
    }"""

ping_new = """    var serverLatencyMs by remember { mutableStateOf<Long?>(null) }
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isLocalConnection by remember { mutableStateOf(true) }
    var activeServerUri by remember(serverUri) { mutableStateOf(serverUri) }

    LaunchedEffect(serverUri, tunnelUri) {
        while (isActive) {
            val client = Fetcher.getInstance().getClient()
            
            suspend fun ping(uri: String): Long? {
                if (uri.isBlank()) return null
                val cleanUri = if (uri.endsWith("/")) uri.dropLast(1) else uri
                val pingUrl = "$cleanUri/api/v1/system/storage"
                return try {
                    val request = okhttp3.Request.Builder().url(pingUrl).head().build()
                    val startTime = System.currentTimeMillis()
                    val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        try { client.newCall(request).execute() } catch (e: Exception) { null }
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    if (response != null && (response.isSuccessful || response.code in 200..499)) {
                        response.close()
                        elapsed
                    } else null
                } catch (e: Exception) { null }
            }

            if (serverUri.isNotBlank()) {
                val localLat = ping(serverUri)
                if (localLat != null) {
                    isServerOnline = true
                    isLocalConnection = true
                    serverLatencyMs = localLat
                    activeServerUri = serverUri
                } else if (!tunnelUri.isNullOrBlank()) {
                    val tunnelLat = ping(tunnelUri!!)
                    if (tunnelLat != null) {
                        isServerOnline = true
                        isLocalConnection = false
                        serverLatencyMs = tunnelLat
                        activeServerUri = tunnelUri!!
                    } else {
                        isServerOnline = false
                        serverLatencyMs = null
                    }
                } else {
                    isServerOnline = false
                    serverLatencyMs = null
                }
            } else {
                isServerOnline = false
                serverLatencyMs = null
            }
            delay(8000L)
        }
    }"""

content = content.replace(ping_old, ping_new)

# 3. Replace usage of serverUri in DriveFilesView, StreamerMediaView, StorageAndHubView, SettingsScreen, etc. with activeServerUri
# But wait, passing activeServerUri to DriveFilesView might cause re-fetching if it changes, which is good.
# Let's replace serverUri = serverUri with serverUri = activeServerUri in the 'when (currentTab)' blocks
content = content.replace("serverUri = serverUri,", "serverUri = activeServerUri,")

# Also replace for Initial load:
content = content.replace("LaunchedEffect(serverUri, effectiveToken)", "LaunchedEffect(activeServerUri, effectiveToken)")
content = content.replace("if (serverUri.isNotBlank() && effectiveToken.isNotBlank())", "if (activeServerUri.isNotBlank() && effectiveToken.isNotBlank())")
content = content.replace('viewModel.fetchFiles(serverUri, effectiveToken, "/", tokenManager = tokenManager)', 'viewModel.fetchFiles(activeServerUri, effectiveToken, "/", tokenManager = tokenManager)')

# Update DriveFilesView signature in AaxionDriveScreen
content = content.replace("""                            onOpenSettings = { currentTab = 3 },
                            isTablet = true""", """                            onOpenSettings = { currentTab = 3 },
                            isTablet = true,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection""")
content = content.replace("""                            onOpenSettings = { currentTab = 3 },
                            isTablet = false""", """                            onOpenSettings = { currentTab = 3 },
                            isTablet = false,
                            isServerOnline = isServerOnline,
                            isLocalConnection = isLocalConnection""")


with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "w") as f:
    f.write(content)
