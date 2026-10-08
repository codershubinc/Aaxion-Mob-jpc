with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/components/InteractiveBreadcrumbs.kt", "r") as f:
    content = f.read()

old_logic = """                    if (lastPart.contains(".") && !targetPath.endsWith("/")) {
                        targetPath = if (lastSlashIdx != -1) targetPath.substring(0, lastSlashIdx) else "/"
                        if (targetPath.isEmpty()) targetPath = "/"
                        Toast.makeText(context, "File path detected. Navigating to parent directory...", Toast.LENGTH_SHORT).show()
                    }"""

new_logic = """                    val dotIndex = lastPart.lastIndexOf('.')
                    val hasValidExtension = if (dotIndex > 0 && dotIndex < lastPart.length - 1) {
                        val ext = lastPart.substring(dotIndex + 1)
                        ext.length in 1..5 && ext.all { it.isLetterOrDigit() }
                    } else false

                    if (hasValidExtension && !targetPath.endsWith("/")) {
                        targetPath = if (lastSlashIdx != -1) targetPath.substring(0, lastSlashIdx) else "/"
                        if (targetPath.isEmpty()) targetPath = "/"
                        Toast.makeText(context, "File path detected. Navigating to parent directory...", Toast.LENGTH_SHORT).show()
                    }"""

content = content.replace(old_logic, new_logic)

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/components/InteractiveBreadcrumbs.kt", "w") as f:
    f.write(content)
