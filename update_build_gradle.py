import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

signing_config = """
    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = "aaxionpassword"
            keyAlias = "key0"
            keyPassword = "aaxionpassword"
        }
    }

    buildTypes {"""

content = content.replace("    buildTypes {", signing_config)

release_type_old = """        release {
            optimization {
                enable = false
            }
        }"""

release_type_new = """        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            optimization {
                enable = false
            }
        }"""

content = content.replace(release_type_old, release_type_new)

with open("app/build.gradle.kts", "w") as f:
    f.write(content)
