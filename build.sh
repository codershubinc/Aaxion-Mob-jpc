#!/usr/bin/env bash

set -euo pipefail

# Directory where the script is located
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${PROJECT_DIR}"

# Read version numbers from version.properties
VERSION_PROPS="${PROJECT_DIR}/version.properties"
VERSION_NAME="0.0.1"
VERSION_CODE="1"

if [ -f "${VERSION_PROPS}" ]; then
    VERSION_NAME="$(grep -E '^(VERSION_NAME|versionName)=' "${VERSION_PROPS}" | cut -d'=' -f2 | tr -d ' "\r\n' || echo "0.0.1")"
    VERSION_CODE="$(grep -E '^(VERSION_CODE|versionCode)=' "${VERSION_PROPS}" | cut -d'=' -f2 | tr -d ' "\r\n' || echo "1")"
fi

OUTPUT_DIR="${PROJECT_DIR}/release-apks"
mkdir -p "${OUTPUT_DIR}"

DO_CLEAN=false
DO_INSTALL=false

for arg in "$@"; do
    case "$arg" in
        --clean)
            DO_CLEAN=true
            ;;
        --install)
            DO_INSTALL=true
            ;;
        *)
            ;;
    esac
done

echo "========================================="
echo " Building AaxionMob Release v${VERSION_NAME} (Build ${VERSION_CODE})"
echo "========================================="

if [ "$DO_CLEAN" = true ]; then
    echo "Executing clean build..."
    ./gradlew clean assembleRelease
else
    ./gradlew assembleRelease
fi

RELEASE_APK_DIR="${PROJECT_DIR}/app/build/outputs/apk/release"
RELEASE_APK="${RELEASE_APK_DIR}/app-release.apk"

if [ ! -f "${RELEASE_APK}" ]; then
    echo "Error: Release APK not found at ${RELEASE_APK}" >&2
    exit 1
fi

TARGET_APK="${OUTPUT_DIR}/AaxionMob-v${VERSION_NAME}.apk"

# Clear previous release apks and copy current release
rm -f "${OUTPUT_DIR}"/*.apk
cp -f "${RELEASE_APK}" "${TARGET_APK}"

echo ""
echo "========================================="
echo " Build Completed Successfully"
echo "========================================="
echo "Release package created:"
echo ""
ls -lh "${TARGET_APK}" | awk '{print "  - " $9 " (" $5 ")"}'
echo ""

if [ "$DO_INSTALL" = true ]; then
    ADB_BIN="$(which adb 2>/dev/null || echo "${HOME}/Android/Sdk/platform-tools/adb")"
    if [ -x "${ADB_BIN}" ]; then
        echo "Installing ${TARGET_APK} onto connected device..."
        if ! "${ADB_BIN}" install -r "${TARGET_APK}"; then
            echo ""
            echo "Note: If installation failed with signature mismatch (INSTALL_FAILED_UPDATE_INCOMPATIBLE),"
            echo "uninstall the existing debug build first before installing the release build:"
            echo "  ${ADB_BIN} uninstall com.codershubinc.aaxion_mob"
            echo "  ${ADB_BIN} install ${TARGET_APK}"
        fi
    else
        echo "adb tool not found; skipping device install."
    fi
fi
