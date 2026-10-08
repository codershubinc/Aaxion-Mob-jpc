import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "r") as f:
    content = f.read()

# Fix DriveFilesView onToggleSidebar to actually toggle
content = content.replace(
    "isSidebarVisible = false,\n                            onToggleSidebar = {},",
    "isSidebarVisible = isSidebarVisible,\n                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },"
)
content = content.replace(
    "isSidebarVisible = false,\n                            onToggleSidebar = {},\n                            isTablet = false",
    "isSidebarVisible = isSidebarVisible,\n                            onToggleSidebar = { isSidebarVisible = !isSidebarVisible },\n                            isTablet = false"
)

# Move the AnimatedVisibility sidebar down
sidebar_start = "                // Collapsible Sidebar with rounded corners (Overlaid with smooth slide, 0 frame drops)"
sidebar_end_str = "            }\n        } else {"
sidebar_start_idx = content.find(sidebar_start)
sidebar_end_idx = content.find(sidebar_end_str)

if sidebar_start_idx != -1 and sidebar_end_idx != -1:
    sidebar_block = content[sidebar_start_idx:sidebar_end_idx]
    
    # Remove from original position
    content = content[:sidebar_start_idx] + content[sidebar_end_idx:]
    
    # Insert it right before the last closing brace of the main Box
    last_box_end = content.rfind("    }\n}")
    if last_box_end != -1:
        content = content[:last_box_end] + "        " + sidebar_block.strip() + "\n" + content[last_box_end:]

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "w") as f:
    f.write(content)
