import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/MainActivity.kt", "r") as f:
    content = f.read()

# Collect tunnel uri
collect_code = """    val savedServerUri by dataStoreManager.serverUri.collectAsState(initial = null)
    val prioritizedServerUri by dataStoreManager.prioritizedServerUri.collectAsState(initial = null)
    val savedTunnelUri by dataStoreManager.tunnelServerUri.collectAsState(initial = null)"""

content = content.replace("""    val savedServerUri by dataStoreManager.serverUri.collectAsState(initial = null)
    val prioritizedServerUri by dataStoreManager.prioritizedServerUri.collectAsState(initial = null)""", collect_code)

# Pass to AaxionDriveScreen
drive_screen_old = """            AaxionDriveScreen(
                serverUri = activeServerUri,
                token = activeAccessToken ?: "","""
drive_screen_new = """            AaxionDriveScreen(
                serverUri = activeServerUri,
                tunnelUri = savedTunnelUri,
                token = activeAccessToken ?: "","""

content = content.replace(drive_screen_old, drive_screen_new)

with open("app/src/main/java/com/codershubinc/aaxion_mob/MainActivity.kt", "w") as f:
    f.write(content)
