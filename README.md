# AaxionMob 🌌

A modern, high-performance client for **Aaxion Server**, built from the ground up with **Jetpack Compose**, **Material 3**, and an **AMOLED Glassmorphic Design System**. Designed for seamless file management, media streaming, and device storage administration across phones, foldables, and tablets.

---

## ✨ Features

### 🗂️ Intelligent Drive & File Management
* **Dual View Modes**: Instant toggle between responsive Grid and compact List layouts.
* **Interactive Breadcrumbs**: One-tap navigation across deep directory trees.
* **Real-time Search & Filter Chips**: Fast substring search with quick-filter pills (All, Media, Documents, Archives).
* **Storage Volume Mounts**: One-tap access to primary host partitions and external USB storage devices with live disk usage indicators.
* **Smart Iconography & Devicons**: 
  - Dynamic file classification powered by external SVG Devicons with local disk caching and rate-limit protection.
  - Native recognition for extensionless files (e.g., `Makefile`, `LICENSE`, `Dockerfile`, `.gitignore`, `.env`).
  - Intelligent regex filtering distinguishing standard system directories from multi-token media and scene release folders.

### 👁️ Built-in File Viewer Suite
* **💻 Code Viewer**: Multi-language syntax highlighting with line numbering, monospaced typography, and horizontal scrolling.
* **📝 Markdown Viewer**: Rich rendering of headings, markdown checklists, blockquotes, code fences, and links.
* **📑 PDF Viewer**: High-resolution page-by-page rendering with smooth zoom and page counter.
* **📄 Text Viewer & Optimizer**: Chunked line-rendering engine (`TextContentOptimizer`) that safely formats and displays large or minified single-line payloads without UI freezing.
* **🖼️ Image Viewer**: Interactive pinch-to-zoom image viewer with native support for animated GIFs and SVG vectors.

### 🌐 Connection & Server Management
* **Live Server Latency**: Real-time ping latency indicators for local IP endpoints and remote tunnel URLs.
* **Local Discovery & Priority Pinning**: Automatic LAN server discovery with support for pinning preferred connection endpoints.
* **Transparent Token Rotation**: Automatic token refresh mechanism that seamlessly recovers expired sessions without interrupting navigation.

### 🎨 AMOLED Glassmorphism & Adaptive Layout
* **Pure AMOLED Dark Canvas**: Deep blacks paired with customizable glass surface brushes and specular accent borders.
* **Responsive Sidebar**: Collapsible slide-in drawer on mobile viewports that automatically dismisses upon item selection; docks as an expanded side rail on larger screens.
* **Seamless Window Insets**: Synchronized window insets ensuring modal sheets never cause notification bar or status bar jumping.

---

## 🏛️ Architecture & Project Structure

The project follows a clean, feature-first modular architecture:

```
app/src/main/java/com/codershubinc/aaxion_mob/
├── core/
│   ├── data/            # AuthRepository, TokenManager, DataStore preferences
│   ├── model/           # FileItem, AuthModels, SystemModels, IconPackTheme
│   ├── network/         # Retrofit services, ApiEndpoints, ServerDiscovery, ServerPingHelper
│   └── util/            # TextContentOptimizer, IntentUtils, DownloadHelper, SystemPaths
├── feature/
│   ├── auth/            # Authentication & Login screens
│   ├── drive/           # Drive files view, action modals, top bar, breadcrumbs, search
│   │   └── viwerModals/ # FileViewerManager, CodeViewerModal, MarkdownViewer, PdfViewerModal, ImageViewerModal
│   ├── settings/        # ConnectionManagerScreen, SettingsScreen, theme preferences
│   ├── storage/         # StorageAndHubView, StorageInfoTile, SidebarStorageDevices
│   └── streamer/        # StreamerMediaView for remote media playback
└── ui/
    ├── designsystem/    # GlassComponents, DeviconHelper, DialogWindowHelper, FileStyleResolver
    └── theme/           # Color, Shape, Theme, Type
```

---

## 🚀 Building & Packaging

### Prerequisites
* JDK 17 or JDK 21
* Gradle 8.x+ (wrapper included)

### Automated Release Build (`build.sh`)
An automated build script is included in the project root to generate optimized, R8-minified, and ABI-split packages:

```bash
# Build release packages
./build.sh

# Perform a clean build and install directly onto a connected device
./build.sh --clean --install
```

### Generated Artifacts
Optimized packages are output to `release-apks/`:
* `app-arm64-v8a-release.apk` (~1.9 MB)
* `app-armeabi-v7a-release.apk` (~1.9 MB)
* `app-x86_64-release.apk` (~1.9 MB)
* `app-universal-release.apk` (~2.0 MB)

### Unified Keystore Signing
Both `debug` and `release` build configurations share the identical keystore (`release.jks`). This enables direct in-place updates (`adb install -r <apk>`) between development and release builds without triggering signature mismatches or clearing application cache, stored credentials, and local preferences.

---

## 🛠️ Tech Stack & Libraries

| Category | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 |
| **UI Toolkit** | Jetpack Compose (Material 3, Compose BOM 2026.02.01) |
| **Architecture** | Modern Android Architecture (ViewModel, StateFlow, Coroutines) |
| **Networking** | Retrofit 2.11.0, OkHttp 4.12.0, Gson |
| **Image Loading** | Coil 2.7.0 (Compose, SVG, GIF support) |
| **Local Storage** | Jetpack DataStore Preferences 1.2.1 |
| **Navigation** | Navigation Compose 2.8.8 |
| **Code Shrinker** | R8 / ProGuard with customized keep rules |

---

## 🧪 Testing

Run local unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License
This project is licensed under the terms of the private project repository. All rights reserved.
