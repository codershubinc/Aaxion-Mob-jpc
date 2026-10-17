# Release Notes — AaxionMob v0.0.1 🚀

**Release Version:** `v0.0.1`  
**Build Code:** `1`  
**Release Date:** October 17, 2026  
**Package:** `AaxionMob-v0.0.1.apk` (~2.0 MB)

---

## 🌟 Overview

Welcome to the initial release of **AaxionMob** (`v0.0.1`)!  
AaxionMob is a modern, high-performance client for **Aaxion Server**, crafted from the ground up with **Jetpack Compose**, **Material 3**, and a custom **AMOLED Glassmorphic Design System**.

---

## 🚀 What's New in v0.0.1

### 🗂️ Drive & File Management
* **Dual Display Layouts**: Seamless toggle between responsive Grid and compact List views.
* **Interactive Breadcrumbs**: One-tap directory traversal through nested folders.
* **Smart Filter Chips & Search**: Real-time debounced file search with instant categorization (All, Media, Documents, Archives).
* **Storage Volume Mounts**: Live partition inspection for primary system storage and external USB drives with visual progress bars and free space meters.
* **Devicon Iconography & Extensionless Recognition**:
  - Dynamic file icons powered by SVG Devicons with local disk caching and rate-limit safeguards.
  - Built-in recognition for extensionless project files (`Makefile`, `LICENSE`, `Dockerfile`, `.gitignore`, `.env`, etc.).
  - Smart folder classification that filters scene releases and media encodes (`1080p`, `WEB-DL`, `.Tv`, `S01`) to preserve clean directory silhouettes.

---

### 👁️ Built-in File Viewer Suite
* **💻 Code Viewer**: Multi-language syntax highlighting with line numbering, monospaced typography, and horizontal code scroll.
* **📝 Markdown Viewer**: Styled document reader supporting headings, checklists, blockquotes, code blocks, and hyperlinks.
* **📑 PDF Viewer**: High-definition page rendering with zoom and gesture navigation.
* **📄 Text Viewer & Optimizer**: Integrated `TextContentOptimizer` that safely formats and chunks huge or minified single-line payloads (such as large JSON exports), eliminating ANRs and UI freezing.
* **🖼️ Image Viewer**: Interactive pinch-to-zoom image viewer with native support for animated GIFs and SVG vectors.

---

### 🌐 Server Discovery & Connectivity
* **Live Latency Monitoring**: Real-time ping latency indicators for local IP addresses and remote tunnel URLs.
* **LAN Auto-Discovery & Pinning**: Automatic discovery of local Aaxion servers with IP priority pinning.
* **Transparent Token Rotation**: Automatic token refresh handling expired sessions without interrupting navigation.

---

### 🎨 AMOLED Glassmorphism & UI Ergonomics
* **Pure AMOLED Dark Canvas**: Deep blacks paired with frosted glass cards, specular accent borders, and dynamic accent colors.
* **Adaptive Sidebar Navigation**: Collapsible slide-in drawer on mobile and tablet devices with automatic auto-hiding upon item or tab selection.
* **Jitter-Free Window Insets**: Synchronized window insets ensuring modal dialogs never trigger status bar or notification bar flicker.

---

### 📦 Build & Packaging
* **Centralized Versioning**: Version codes and names managed via `version.properties`.
* **Single Universal Release APK**: Consolidated into a single, compact `AaxionMob-v0.0.1.apk` (2.0 MB) with R8 minification and resource shrinking.
* **Unified Keystore Signing**: Shared keystore across debug and release build variants, enabling in-place updates (`adb install -r`) without clearing local app data, cached files, or login sessions.

---

## 📲 Installation

Download `AaxionMob-v0.0.1.apk` from the `release-apks/` directory or build directly from source:

```bash
# Compile and output single universal release APK
./build.sh

# Build and install directly onto a connected device
./build.sh --install
```

---

## 📋 Compatibility
* **Minimum Android Version:** Android 8.0 (API level 26 - Oreo)
* **Target Android Version:** Android 15 (API level 37)
* **Supported ABIs:** `arm64-v8a`, `armeabi-v7a`, `x86_64` (Universal APK)
