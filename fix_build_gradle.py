import re
with open("app/build.gradle.kts", "r") as f:
    content = f.read()

content = content.replace("isMinifyEnabled = true", "isMinifyEnabled = false")
content = content.replace("isShrinkResources = true", "isShrinkResources = false")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
