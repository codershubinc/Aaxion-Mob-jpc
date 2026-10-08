#!/bin/bash
echo "Building Release APK..."
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    echo "========================================="
    echo "Build Successful!"
    echo "Release APK is located at: app/build/outputs/apk/release/app-release.apk"
    echo "========================================="
else
    echo "========================================="
    echo "Build Failed! Check the logs above."
    echo "========================================="
    exit 1
fi
