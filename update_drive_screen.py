import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "r") as f:
    content = f.read()

# We need to change DriveFilesView to receive the real isSidebarVisible instead of false for mobile
content = content.replace("isSidebarVisible = false,\n                            onToggleSidebar = {},", "isSidebarVisible = isSidebarVisible,\n                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },")

# Now, we extract the sidebar to a variable/composable? Or we just duplicate it into the mobile box.
# Actually, the best way is to take the AnimatedVisibility block and put it at the very bottom of the main Box, after the if/else block!
# But wait, it's currently inside `if (!isMobile) { Box { ... AnimatedVisibility } }`.
# If I move it out, it will show on top of everything.

sidebar_start = "                // Collapsible Sidebar with rounded corners (Overlaid with smooth slide, 0 frame drops)"
sidebar_end = "            }\n        } else {"
sidebar_block = content[content.find(sidebar_start):content.find(sidebar_end)]

# Remove it from tablet layout
content = content.replace(sidebar_block, "")

# Find the end of the mobile layout
# } else {
#     Scaffold { ... }
# }

mobile_end = "                    }\n                }\n            }\n        }\n    }\n}"
new_mobile_end = "                    }\n                }\n            }\n        }\n" + sidebar_block + "\n    }\n}"

content = content.replace(mobile_end, new_mobile_end)

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "w") as f:
    f.write(content)

