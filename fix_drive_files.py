import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/views/DriveFilesView.kt", "r") as f:
    content = f.read()

sig_old = """    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    onOpenSettings: () -> Unit,
    isTablet: Boolean"""
sig_new = """    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    onOpenSettings: () -> Unit,
    isTablet: Boolean,
    isServerOnline: Boolean?,
    isLocalConnection: Boolean"""
content = content.replace(sig_old, sig_new)

# Add indicator in the top right, before Settings icon.
indicator_code = """
                    if (!isTablet) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isServerOnline == true) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = "Connection Status",
                            tint = if (isServerOnline == true) {
                                if (isLocalConnection) AaxionPrimary else StreamerOrange
                            } else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
"""

old_settings_icon = """                    if (!isTablet) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }"""

content = content.replace(old_settings_icon, indicator_code)

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/views/DriveFilesView.kt", "w") as f:
    f.write(content)
