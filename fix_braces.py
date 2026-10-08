with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "r") as f:
    content = f.read()

# Replace the messy end braces
bad_end = "    }\n}}\n}"
good_end = "    }\n}"

content = content.replace("    }\n}}\n}\n\n// Tablet Sidebar Item", "    }\n}\n\n// Tablet Sidebar Item")
with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/AaxionDriveScreen.kt", "w") as f:
    f.write(content)

