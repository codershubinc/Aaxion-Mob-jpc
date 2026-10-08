import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "r") as f:
    content = f.read()

target = "                // Collapsible Sidebar with rounded corners (Overlaid with smooth slide, 0 frame drops)"

overlay_code = """
        // Dimmed overlay for mobile/tablet to tap and close the sidebar
        if (!isDesktop) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSidebarVisible,
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(150)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { isSidebarVisible = false }
                )
            }
        }
"""

content = content.replace(target, overlay_code.lstrip('\n') + target)

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "w") as f:
    f.write(content)
