#!/usr/bin/env bash

set -euo pipefail

# Directory where the script is located
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${PROJECT_DIR}"

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
echo " Building Optimized Release Packages"
echo "========================================="

if [ "$DO_CLEAN" = true ]; then
    echo "Executing clean build..."
    ./gradlew clean assembleRelease
else
    ./gradlew assembleRelease
fi

RELEASE_APK_DIR="${PROJECT_DIR}/app/build/outputs/apk/release"

if [ ! -d "${RELEASE_APK_DIR}" ]; then
    echo "Error: Output directory not found at ${RELEASE_APK_DIR}" >&2
    exit 1
fi

echo ""
echo "Copying release packages to ${OUTPUT_DIR}/..."
cp -f "${RELEASE_APK_DIR}"/*.apk "${OUTPUT_DIR}/"

echo ""
echo "========================================="
echo " Build Completed Successfully"
echo "========================================="
echo "Optimized packages available in ${OUTPUT_DIR}:"
echo ""
ls -lh "${OUTPUT_DIR}"/*.apk | awk '{print "  - " $9 " (" $5 ")"}'
echo ""

if [ "$DO_INSTALL" = true ]; then
    ADB_BIN="$(which adb 2>/dev/null || echo "${HOME}/Android/Sdk/platform-tools/adb")"
    if [ -x "${ADB_BIN}" ]; then
        TARGET_APK="${OUTPUT_DIR}/app-universal-release.apk"
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
