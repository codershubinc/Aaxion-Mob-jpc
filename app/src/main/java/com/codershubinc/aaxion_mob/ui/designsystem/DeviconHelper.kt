package com.codershubinc.aaxion_mob.ui.designsystem

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale

/**
 * Devicon SVG URL resolver and persistent offline-first cache manager.
 * Guarantees that icons are downloaded at most once and cached permanently on device,
 * preventing any CDN rate-limiting issues.
 */
object DeviconHelper {

    private const val DEVICON_BASE_URL = "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons"
    private var imageLoader: ImageLoader? = null

    /**
     * Resolves the official Devicon SVG URL for the specified file name and extension.
     */
    fun getDeviconUrl(fileName: String, extension: String): String? {
        val ext = extension.lowercase(Locale.ROOT)
        val lowerName = fileName.lowercase(Locale.ROOT)

        val iconPath = when {
            // Kotlin
            ext in setOf("kt", "kts") -> "kotlin/kotlin-original.svg"
            // TypeScript
            ext in setOf("ts", "tsx") -> "typescript/typescript-original.svg"
            // JavaScript
            ext in setOf("js", "jsx", "mjs", "cjs") -> "javascript/javascript-original.svg"
            // Python
            ext in setOf("py", "pyw") -> "python/python-original.svg"
            // Java
            ext == "java" -> "java/java-original.svg"
            // Rust
            ext == "rs" -> "rust/rust-original.svg"
            // Go
            ext == "go" -> "go/go-original.svg"
            // C++
            ext in setOf("cpp", "cc", "cxx", "hpp") -> "cplusplus/cplusplus-original.svg"
            // C
            ext in setOf("c", "h") -> "c/c-original.svg"
            // C#
            ext == "cs" -> "csharp/csharp-original.svg"
            // HTML
            ext in setOf("html", "htm") -> "html5/html5-original.svg"
            // CSS
            ext in setOf("css", "scss", "sass", "less") -> "css3/css3-original.svg"
            // JSON
            ext == "json" -> "json/json-original.svg"
            // Markdown & Readme
            ext in setOf("md", "markdown") || lowerName.startsWith("readme") -> "markdown/markdown-original.svg"
            // Docker
            ext == "dockerfile" || lowerName == "dockerfile" || lowerName == "containerfile" || lowerName == ".dockerignore" -> "docker/docker-original.svg"
            // Git
            lowerName.startsWith(".git") -> "git/git-original.svg"
            // Shell & Make
            ext in setOf("sh", "bash", "zsh") || lowerName in setOf("makefile", "gnumakefile", ".bashrc", ".zshrc", ".profile") -> "bash/bash-original.svg"
            // YAML
            ext in setOf("yaml", "yml") -> "yaml/yaml-original.svg"
            // SQL
            ext == "sql" -> "mysql/mysql-original.svg"
            // PHP
            ext == "php" -> "php/php-original.svg"
            // Ruby
            ext == "rb" || lowerName in setOf("gemfile", "rakefile") -> "ruby/ruby-original.svg"
            // Swift
            ext == "swift" -> "swift/swift-original.svg"
            // Dart
            ext == "dart" -> "dart/dart-original.svg"
            // Lua
            ext == "lua" -> "lua/lua-original.svg"
            else -> null
        }

        return iconPath?.let { "$DEVICON_BASE_URL/$it" }
    }

    /**
     * Returns a singleton [ImageLoader] configured with [SvgDecoder] and aggressive disk caching.
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: buildImageLoader(context.applicationContext).also { imageLoader = it }
        }
    }

    private fun buildImageLoader(appContext: Context): ImageLoader {
        val persistentCacheDir = File(appContext.filesDir, "devicons_persistent_cache")
        if (!persistentCacheDir.exists()) {
            persistentCacheDir.mkdirs()
        }

        // Custom OkHttpClient with 50MB disk cache and year-long cache-control header rewrite
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(persistentCacheDir, 50L * 1024 * 1024))
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=31536000, immutable")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()

        return ImageLoader.Builder(appContext)
            .components {
                add(SvgDecoder.Factory())
            }
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.filesDir, "devicons_coil_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false) // Cache indefinitely regardless of remote headers
            .build()
    }
}
