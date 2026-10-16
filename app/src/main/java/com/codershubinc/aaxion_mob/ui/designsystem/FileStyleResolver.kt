package com.codershubinc.aaxion_mob.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.model.IconPackTheme
import com.codershubinc.aaxion_mob.ui.theme.*

// Top-level immutable file extension sets to avoid allocations during list rendering
private val ARCHIVE_EXTS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "iso", "xz", "zst", "dmg")
private val DB_EXTS = setOf("db", "sqlite", "sqlite3", "mdb", "parquet")
private val BOOK_EXTS = setOf("epub", "mobi", "azw3", "cbz", "cbr")
private val FONT_EXTS = setOf("ttf", "otf", "woff", "woff2")
private val SPREADSHEET_EXTS = setOf("xls", "xlsx", "csv", "tsv", "ods")
private val PRESENTATION_EXTS = setOf("ppt", "pptx", "odp", "key")
private val WORD_EXTS = setOf("doc", "docx", "odt")
private val TEXT_EXTS = setOf("txt", "md", "rtf", "log", "conf", "cfg", "ini")
private val CODE_EXTS = setOf(
    "json", "js", "ts", "jsx", "tsx", "go", "rs", "py", "java", "kt", "kts",
    "cpp", "c", "h", "hpp", "cs", "html", "css", "scss", "sh", "bash", "zsh",
    "yaml", "yml", "xml", "sql", "php", "rb", "swift", "dart", "lua", "toml", "env"
)
private val KNOWN_CODE_FILENAMES = setOf(
    "makefile", "gnumakefile", "dockerfile", "containerfile", "gemfile",
    "procfile", "vagrantfile", "rakefile", "brewfile", "cmakelists.txt"
)
private val KNOWN_DOC_FILENAMES = setOf(
    "license", "licence", "readme", "changelog", "authors", "contributors",
    "copying", "notice", "patents", "todo", "version"
)

// Regex patterns detecting scene / media releases and web-domain folders
private val MEDIA_RELEASE_REGEX = Regex(
    """(?i)(\b|_|\.)(s\d{1,2}(e\d{1,3})?|season[ ._-]?\d+|episode[ ._-]?\d+|\d{3,4}p|4k|8k|uhd|fhd|bluray|blu-ray|web-?dl|webrip|hdtv|dvdrip|brrip|bdrip|hevc|x264|x265|h\.?264|h\.?265|10bit|remux|proper|repack|aac\d?|ddp\d?|dts)(\b|_|\.)"""
)
private val DOMAIN_SUFFIX_REGEX = Regex(
    """(?i)\.(com|tv|org|net|in|cc|to|hub|biz|io|co|me|vip|info)(\b|_|-|$)"""
)

// Top-level immutable directory name sets
private val CONTAINER_DIR_NAMES = setOf("docker", "containers", "container", "k8s", "kubernetes")
private val BACKUP_DIR_NAMES = setOf("backups", "backup", "time machine", "archive", "archives")
private val CAMERA_DIR_NAMES = setOf("camera", "dcim")
private val PODCAST_DIR_NAMES = setOf("podcasts", "podcast", "audiobooks", "audiobook")
private val FONT_DIR_NAMES = setOf("fonts", "font")
private val DOWNLOAD_DIR_NAMES = setOf("downloads", "download")
private val SERIES_DIR_NAMES = setOf("series", "tv series", "tv shows", "web series", "anime")
private val VIDEO_DIR_NAMES = setOf("videos", "movies", "video", "movie", "films", "film")
private val MUSIC_DIR_NAMES = setOf("music", "audio", "songs", "song")
private val PHOTO_DIR_NAMES = setOf("pictures", "photos", "images", "screenshots", "wallpaper", "wallpapers")
private val DOC_DIR_NAMES = setOf("documents", "document", "docs")
private val DEV_DIR_NAMES = setOf("github", "git", ".git", "projects", "repo", "repositories", "workspace", "workspaces")
private val PLATFORM_DIR_NAMES = setOf("android", ".android")
private val HARDWARE_DIR_NAMES = setOf("arduino")
private val LIBRARY_DIR_NAMES = setOf("library", "books")
private val PUBLIC_DIR_NAMES = setOf("public", "shared")
private val TEMPLATE_DIR_NAMES = setOf("templates", "template")
private val APP_DIR_NAMES = setOf("snap", "apps", "applications")
private val GOLANG_DIR_NAMES = setOf("golang")
private val BUILD_DIR_NAMES = setOf("build", "dist")
private val TEMP_DIR_NAMES = setOf("uploads_temp", "temp", "tmp", "cache", ".cache")

data class FileVisualStyle(
    val icon: ImageVector,
    val tint: Color,
    val containerBrush: Brush,
    val badgeText: String? = null,
    val folderEmblem: ImageVector? = null,
    val isFolder: Boolean = false,
    val deviconUrl: String? = null
)

private fun brushForTint(tint: Color): Brush {
    return Brush.linearGradient(
        colors = listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.06f)),
        start = Offset.Zero,
        end = Offset.Infinite
    )
}

/**
 * Resolves visual representation (icon glyph, tint, container brush, and badge)
 * based on file type, extension, active accent color, and chosen IconPackTheme.
 * Memoized across recompositions to eliminate allocations during fast scrolling.
 */
@Composable
fun resolveFileStyle(
    file: FileItem,
    accentColor: Color = LocalAccentColor.current,
    iconPack: IconPackTheme = LocalIconPack.current
): FileVisualStyle {
    return remember(file.rawPath, file.isDir, accentColor, iconPack) {
        computeFileStyle(file, accentColor, iconPack)
    }
}

internal fun computeFileStyle(
    file: FileItem,
    accentColor: Color,
    iconPack: IconPackTheme
): FileVisualStyle {
    val ext = file.extension.lowercase(java.util.Locale.ROOT)
    val lowerName = file.name.lowercase(java.util.Locale.ROOT)
    val isDotCode = lowerName.startsWith('.') && (lowerName.endsWith("rc") || lowerName.endsWith("profile") || lowerName in setOf(".gitignore", ".gitattributes", ".gitmodules", ".env", ".editorconfig", ".prettierrc", ".eslintrc", ".npmrc", ".dockerignore"))
    val isCode = ext in CODE_EXTS || lowerName in KNOWN_CODE_FILENAMES || isDotCode
    val isMarkdown = ext in setOf("md", "markdown") || lowerName.startsWith("readme")
    val isDoc = ext in TEXT_EXTS || ext in WORD_EXTS || lowerName in KNOWN_DOC_FILENAMES

    return when {
        file.isDir -> resolveDirectoryStyle(file, accentColor, iconPack)

        file.isVideo -> {
            val vidIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.PlayCircle
                IconPackTheme.OUTLINE -> Icons.Outlined.PlayCircleOutline
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.PlayCircle
                IconPackTheme.CYBER_NEON -> Icons.Default.SlowMotionVideo
            }
            val vidTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFF5252) else StreamerOrange
            FileVisualStyle(
                icon = vidIcon,
                tint = vidTint,
                containerBrush = brushForTint(vidTint),
                badgeText = "VID"
            )
        }

        file.isAudio -> {
            val audIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.MusicNote
                IconPackTheme.OUTLINE -> Icons.Outlined.MusicNote
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.MusicNote
                IconPackTheme.CYBER_NEON -> Icons.Default.Equalizer
            }
            val audTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF00E676) else StreamerGreen
            FileVisualStyle(
                icon = audIcon,
                tint = audTint,
                containerBrush = brushForTint(audTint),
                badgeText = "AUD"
            )
        }

        file.isImage -> {
            val imgIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Image
                IconPackTheme.OUTLINE -> Icons.Outlined.Image
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Image
                IconPackTheme.CYBER_NEON -> Icons.Default.FilterHdr
            }
            val imgTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFF4081) else StreamerPink
            FileVisualStyle(
                icon = imgIcon,
                tint = imgTint,
                containerBrush = brushForTint(imgTint),
                badgeText = "IMG"
            )
        }

        // Package installer files
        ext == "apk" -> {
            val apkIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Android
                IconPackTheme.OUTLINE -> Icons.Outlined.Android
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Android
                IconPackTheme.CYBER_NEON -> Icons.Default.Android
            }
            val apkTint = Color(0xFF30D158)
            FileVisualStyle(
                icon = apkIcon,
                tint = apkTint,
                containerBrush = brushForTint(apkTint),
                badgeText = "APK"
            )
        }

        // Compressed archive files
        ext in ARCHIVE_EXTS -> {
            val arcIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Archive
                IconPackTheme.OUTLINE -> Icons.Outlined.Archive
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Archive
                IconPackTheme.CYBER_NEON -> Icons.Default.Archive
            }
            val arcTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFBBF24) else Color(0xFFF59E0B)
            FileVisualStyle(
                icon = arcIcon,
                tint = arcTint,
                containerBrush = brushForTint(arcTint),
                badgeText = ext.uppercase().take(4)
            )
        }

        // Database and structured storage files
        ext in DB_EXTS -> {
            val dbIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Storage
                IconPackTheme.OUTLINE -> Icons.Outlined.Storage
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Storage
                IconPackTheme.CYBER_NEON -> Icons.Default.Storage
            }
            val dbTint = Color(0xFF06B6D4)
            FileVisualStyle(
                icon = dbIcon,
                tint = dbTint,
                containerBrush = brushForTint(dbTint),
                badgeText = "DB"
            )
        }

        // E-books and digital publications
        ext in BOOK_EXTS -> {
            val bookIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.AutoMirrored.Rounded.MenuBook
                IconPackTheme.OUTLINE -> Icons.AutoMirrored.Outlined.MenuBook
                IconPackTheme.SYSTEM_FILLED -> Icons.AutoMirrored.Filled.MenuBook
                IconPackTheme.CYBER_NEON -> Icons.AutoMirrored.Filled.MenuBook
            }
            val bookTint = Color(0xFFF59E0B)
            FileVisualStyle(
                icon = bookIcon,
                tint = bookTint,
                containerBrush = brushForTint(bookTint),
                badgeText = "BOOK"
            )
        }

        // Font and typography files
        ext in FONT_EXTS -> {
            val fontIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.TextFields
                IconPackTheme.OUTLINE -> Icons.Outlined.TextFields
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.TextFields
                IconPackTheme.CYBER_NEON -> Icons.Default.TextFields
            }
            val fontTint = Color(0xFF94A3B8)
            FileVisualStyle(
                icon = fontIcon,
                tint = fontTint,
                containerBrush = brushForTint(fontTint),
                badgeText = "FONT"
            )
        }

        // PDF Documents
        ext == "pdf" -> {
            val pdfIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.PictureAsPdf
                IconPackTheme.OUTLINE -> Icons.Outlined.PictureAsPdf
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.PictureAsPdf
                IconPackTheme.CYBER_NEON -> Icons.Default.PictureAsPdf
            }
            val pdfTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFF5252) else Color(0xFFEF4444)
            FileVisualStyle(
                icon = pdfIcon,
                tint = pdfTint,
                containerBrush = brushForTint(pdfTint),
                badgeText = "PDF"
            )
        }

        // Spreadsheets & Tabular Data
        ext in SPREADSHEET_EXTS -> {
            val sheetIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.TableChart
                IconPackTheme.OUTLINE -> Icons.Outlined.TableChart
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.TableChart
                IconPackTheme.CYBER_NEON -> Icons.Default.TableChart
            }
            val sheetTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF00E676) else Color(0xFF10B981)
            FileVisualStyle(
                icon = sheetIcon,
                tint = sheetTint,
                containerBrush = brushForTint(sheetTint),
                badgeText = ext.uppercase().take(4)
            )
        }

        // Presentations
        ext in PRESENTATION_EXTS -> {
            val pptIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Slideshow
                IconPackTheme.OUTLINE -> Icons.Outlined.Slideshow
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Slideshow
                IconPackTheme.CYBER_NEON -> Icons.Default.Slideshow
            }
            val pptTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFF9100) else StreamerOrange
            FileVisualStyle(
                icon = pptIcon,
                tint = pptTint,
                containerBrush = brushForTint(pptTint),
                badgeText = ext.uppercase().take(4)
            )
        }

        // Code, Scripts & Data Models (including dotless files like makefile, dockerfile, .gitignore)
        isCode -> {
            val codeIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Code
                IconPackTheme.OUTLINE -> Icons.Outlined.Code
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Code
                IconPackTheme.CYBER_NEON -> Icons.Default.Terminal
            }
            val codeTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF00E5FF) else Color(0xFF818CF8)
            val badge = if (ext.isNotEmpty()) ext.uppercase().take(4) else lowerName.removePrefix(".").uppercase().take(4)
            val devicon = DeviconHelper.getDeviconUrl(file.name, ext)
            FileVisualStyle(
                icon = codeIcon,
                tint = codeTint,
                containerBrush = brushForTint(codeTint),
                badgeText = badge,
                deviconUrl = devicon
            )
        }

        // Markdown & Readme Documents
        isMarkdown -> {
            val mdIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.AutoMirrored.Rounded.Article
                IconPackTheme.OUTLINE -> Icons.AutoMirrored.Outlined.Article
                IconPackTheme.SYSTEM_FILLED -> Icons.AutoMirrored.Filled.Article
                IconPackTheme.CYBER_NEON -> Icons.AutoMirrored.Filled.Article
            }
            val mdTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFA78BFA) else Color(0xFF60A5FA)
            val devicon = DeviconHelper.getDeviconUrl(file.name, ext)
            FileVisualStyle(
                icon = mdIcon,
                tint = mdTint,
                containerBrush = brushForTint(mdTint),
                badgeText = if (ext.isNotEmpty()) ext.uppercase().take(4) else "MD",
                deviconUrl = devicon
            )
        }

        // Word Processing & Documents
        ext in WORD_EXTS -> {
            val docIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Description
                IconPackTheme.OUTLINE -> Icons.Outlined.Description
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Description
                IconPackTheme.CYBER_NEON -> Icons.AutoMirrored.Filled.Article
            }
            val docTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF60A5FA) else Color(0xFF3B82F6)
            FileVisualStyle(
                icon = docIcon,
                tint = docTint,
                containerBrush = brushForTint(docTint),
                badgeText = ext.uppercase().take(4)
            )
        }

        // Plain Text & Documents (including dotless files like license, licence, changelog)
        isDoc -> {
            val txtIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.AutoMirrored.Rounded.Article
                IconPackTheme.OUTLINE -> Icons.AutoMirrored.Outlined.Article
                IconPackTheme.SYSTEM_FILLED -> Icons.AutoMirrored.Filled.Article
                IconPackTheme.CYBER_NEON -> Icons.AutoMirrored.Filled.Article
            }
            val txtTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFA78BFA) else Color(0xFF94A3B8)
            val badge = if (ext.isNotEmpty()) ext.uppercase().take(4) else lowerName.uppercase().take(4)
            FileVisualStyle(
                icon = txtIcon,
                tint = txtTint,
                containerBrush = brushForTint(txtTint),
                badgeText = badge
            )
        }

        // Generic Fallback
        else -> {
            val genericIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.AutoMirrored.Rounded.InsertDriveFile
                IconPackTheme.OUTLINE -> Icons.AutoMirrored.Outlined.InsertDriveFile
                IconPackTheme.SYSTEM_FILLED -> Icons.AutoMirrored.Filled.InsertDriveFile
                IconPackTheme.CYBER_NEON -> Icons.Default.DataObject
            }
            val genericTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF00F0FF) else TextSecondary
            val extBadge = ext.uppercase().take(4).ifEmpty { "FILE" }
            FileVisualStyle(
                icon = genericIcon,
                tint = genericTint,
                containerBrush = brushForTint(Color(0xFF22222E)),
                badgeText = extBadge
            )
        }
    }
}

fun IconPackTheme.previewFolderIcon(): ImageVector = when (this) {
    IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Folder
    IconPackTheme.OUTLINE -> Icons.Outlined.Folder
    IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Folder
    IconPackTheme.CYBER_NEON -> Icons.Default.FolderSpecial
}

/**
 * Renders the file or directory iconography.
 * For directories with emblems (e.g. Downloads, Series, Pictures, Videos), it renders an authentic
 * folder icon silhouette with the specific category emblem centered on its front flap.
 */
@Composable
fun FileVisualIcon(
    fileStyle: FileVisualStyle,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    if (fileStyle.isFolder && fileStyle.folderEmblem != null) {
        val currentIconPack = LocalIconPack.current
        val emblemSize = if (currentIconPack == IconPackTheme.OUTLINE) iconSize * 0.38f else iconSize * 0.44f
        val emblemOffsetY = iconSize * 0.08f
        val emblemColor = when (currentIconPack) {
            IconPackTheme.OUTLINE -> fileStyle.tint
            else -> Color(0xFF0B0B0F).copy(alpha = 0.88f)
        }

        Box(
            modifier = modifier.size(iconSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fileStyle.icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = fileStyle.tint
            )
            Icon(
                imageVector = fileStyle.folderEmblem,
                contentDescription = null,
                modifier = Modifier
                    .size(emblemSize)
                    .offset(y = emblemOffsetY),
                tint = emblemColor
            )
        }
    } else if (fileStyle.deviconUrl != null) {
        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(fileStyle.deviconUrl)
                .crossfade(true)
                .build(),
            imageLoader = DeviconHelper.getImageLoader(context),
            contentDescription = null,
            modifier = modifier.size(iconSize),
            loading = {
                Icon(
                    imageVector = fileStyle.icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = fileStyle.tint
                )
            },
            error = {
                Icon(
                    imageVector = fileStyle.icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = fileStyle.tint
                )
            }
        )
    } else {
        Icon(
            imageVector = fileStyle.icon,
            contentDescription = null,
            modifier = modifier.size(iconSize),
            tint = fileStyle.tint
        )
    }
}

/**
 * Checks directory names and resolves special folder iconography, emblems, and accent tints.
 * Ensures directories always retain an authentic folder silhouette.
 */
private fun resolveDirectoryStyle(
    file: FileItem,
    accentColor: Color,
    iconPack: IconPackTheme
): FileVisualStyle {
    val rawName = file.name.trim()
    val cleanName = rawName.lowercase(java.util.Locale.ROOT)
    val genericFolderIcon = iconPack.previewFolderIcon()
    val defaultFolderTint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF38BDF8) else accentColor

    // 1. Scene releases, episodic seasons, media encodes, or domain-tagged directories
    // must always display as standard folders rather than special category directories.
    if (MEDIA_RELEASE_REGEX.containsMatchIn(rawName) || DOMAIN_SUFFIX_REGEX.containsMatchIn(rawName)) {
        return FileVisualStyle(
            icon = genericFolderIcon,
            tint = defaultFolderTint,
            containerBrush = brushForTint(defaultFolderTint),
            badgeText = null,
            folderEmblem = null,
            isFolder = true
        )
    }

    val normalized = cleanName.replace(Regex("[_.-]+"), " ").trim()

    // Matches standard directory names exactly or prefixed with "my" (e.g. "my downloads", "tv series")
    fun matches(set: Set<String>): Boolean {
        if (cleanName in set || normalized in set) return true
        for (item in set) {
            if (normalized == "my $item" || normalized == "my ${item}s") return true
        }
        return false
    }

    // If icon pack uses a decorated folder silhouette by default, use a clean folder canvas
    // when an emblem is present to avoid graphic collision on the front flap.
    val baseFolderIcon = if (iconPack == IconPackTheme.CYBER_NEON) {
        Icons.Rounded.Folder
    } else {
        iconPack.previewFolderIcon()
    }

    val (folderIcon, folderEmblem, tint) = when {
        // Containerization and cluster environments
        matches(CONTAINER_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Dns
                IconPackTheme.OUTLINE -> Icons.Outlined.Dns
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Dns
                IconPackTheme.CYBER_NEON -> Icons.Default.Dns
            }
            Triple(baseFolderIcon, emblem, Color(0xFF0EA5E9))
        }

        // Backups and archive snapshots
        matches(BACKUP_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Restore
                IconPackTheme.OUTLINE -> Icons.Outlined.Restore
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Restore
                IconPackTheme.CYBER_NEON -> Icons.Default.Restore
            }
            Triple(baseFolderIcon, emblem, Color(0xFFF59E0B))
        }

        // Downloads
        matches(DOWNLOAD_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Download
                IconPackTheme.OUTLINE -> Icons.Outlined.Download
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Download
                IconPackTheme.CYBER_NEON -> Icons.Default.Download
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF00E5FF) else Color(0xFF06B6D4)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Series and episodic shows
        matches(SERIES_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Tv
                IconPackTheme.OUTLINE -> Icons.Outlined.Tv
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Tv
                IconPackTheme.CYBER_NEON -> Icons.Default.LiveTv
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFD946EF) else Color(0xFFA855F7)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Videos and film collections
        matches(VIDEO_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Movie
                IconPackTheme.OUTLINE -> Icons.Outlined.Movie
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Movie
                IconPackTheme.CYBER_NEON -> Icons.Default.SlowMotionVideo
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFF5252) else StreamerOrange
            Triple(baseFolderIcon, emblem, tint)
        }

        // Spoken audio and podcast collections
        matches(PODCAST_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Headphones
                IconPackTheme.OUTLINE -> Icons.Outlined.Headphones
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Headphones
                IconPackTheme.CYBER_NEON -> Icons.Default.Headphones
            }
            Triple(baseFolderIcon, emblem, StreamerGreen)
        }

        // Music and general audio tracks
        matches(MUSIC_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.MusicNote
                IconPackTheme.OUTLINE -> Icons.Outlined.MusicNote
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.MusicNote
                IconPackTheme.CYBER_NEON -> Icons.Default.Equalizer
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF00E676) else StreamerGreen
            Triple(baseFolderIcon, emblem, tint)
        }

        // Camera capture and snapshot repositories
        matches(CAMERA_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.PhotoCamera
                IconPackTheme.OUTLINE -> Icons.Outlined.PhotoCamera
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.PhotoCamera
                IconPackTheme.CYBER_NEON -> Icons.Default.PhotoCamera
            }
            Triple(baseFolderIcon, emblem, StreamerPink)
        }

        // Pictures and image albums
        matches(PHOTO_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Image
                IconPackTheme.OUTLINE -> Icons.Outlined.Image
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Image
                IconPackTheme.CYBER_NEON -> Icons.Default.FilterHdr
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFF4081) else StreamerPink
            Triple(baseFolderIcon, emblem, tint)
        }

        // Document archives
        matches(DOC_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Description
                IconPackTheme.OUTLINE -> Icons.Outlined.Description
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Description
                IconPackTheme.CYBER_NEON -> Icons.AutoMirrored.Filled.Article
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF60A5FA) else Color(0xFF3B82F6)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Typography and typeface collections
        matches(FONT_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.FontDownload
                IconPackTheme.OUTLINE -> Icons.Outlined.FontDownload
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.FontDownload
                IconPackTheme.CYBER_NEON -> Icons.Default.FontDownload
            }
            Triple(baseFolderIcon, emblem, Color(0xFF94A3B8))
        }

        // Desktop workspace directory
        cleanName == "desktop" -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.DesktopWindows
                IconPackTheme.OUTLINE -> Icons.Outlined.DesktopWindows
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.DesktopWindows
                IconPackTheme.CYBER_NEON -> Icons.Default.Computer
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFC084FC) else Color(0xFF8B5CF6)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Mobile development environment
        cleanName in PLATFORM_DIR_NAMES -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Android
                IconPackTheme.OUTLINE -> Icons.Outlined.Android
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Android
                IconPackTheme.CYBER_NEON -> Icons.Default.Android
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF4ADE80) else Color(0xFF22C55E)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Microcontroller and embedded development
        cleanName in HARDWARE_DIR_NAMES -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.DeveloperBoard
                IconPackTheme.OUTLINE -> Icons.Outlined.DeveloperBoard
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.DeveloperBoard
                IconPackTheme.CYBER_NEON -> Icons.Default.Memory
            }
            Triple(baseFolderIcon, emblem, Color(0xFF00979D))
        }

        // Repositories and source projects
        matches(DEV_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Code
                IconPackTheme.OUTLINE -> Icons.Outlined.Code
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Code
                IconPackTheme.CYBER_NEON -> Icons.Default.Terminal
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFA78BFA) else Color(0xFF818CF8)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Publications and library volumes
        matches(LIBRARY_DIR_NAMES) -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.LocalLibrary
                IconPackTheme.OUTLINE -> Icons.Outlined.LocalLibrary
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.LocalLibrary
                IconPackTheme.CYBER_NEON -> Icons.Default.CollectionsBookmark
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFDE047) else Color(0xFFEAB308)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Public and distributed shares
        cleanName in PUBLIC_DIR_NAMES -> {
            val sharedFolderIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.FolderShared
                IconPackTheme.OUTLINE -> Icons.Outlined.FolderShared
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.FolderShared
                IconPackTheme.CYBER_NEON -> Icons.Default.FolderShared
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFF38BDF8) else Color(0xFF0EA5E9)
            Triple(sharedFolderIcon, null, tint)
        }

        // Project blueprints and templates
        cleanName in TEMPLATE_DIR_NAMES -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.ContentPaste
                IconPackTheme.OUTLINE -> Icons.Outlined.ContentPaste
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.ContentPaste
                IconPackTheme.CYBER_NEON -> Icons.Default.ContentCopy
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFFBBF24) else Color(0xFFF59E0B)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Application binaries and packages
        cleanName in APP_DIR_NAMES -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Apps
                IconPackTheme.OUTLINE -> Icons.Outlined.Apps
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Apps
                IconPackTheme.CYBER_NEON -> Icons.Default.Widgets
            }
            val tint = if (iconPack == IconPackTheme.CYBER_NEON) Color(0xFFF43F5E) else Color(0xFFE11D48)
            Triple(baseFolderIcon, emblem, tint)
        }

        // Language packages and schemas
        cleanName in GOLANG_DIR_NAMES -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.DataObject
                IconPackTheme.OUTLINE -> Icons.Outlined.DataObject
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.DataObject
                IconPackTheme.CYBER_NEON -> Icons.Default.DataObject
            }
            Triple(baseFolderIcon, emblem, Color(0xFF00ADD8))
        }

        // Output artifacts and builds
        cleanName in BUILD_DIR_NAMES -> {
            val emblem = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.Build
                IconPackTheme.OUTLINE -> Icons.Outlined.Build
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.Build
                IconPackTheme.CYBER_NEON -> Icons.Default.Handyman
            }
            Triple(baseFolderIcon, emblem, Color(0xFF94A3B8))
        }

        // Ephemeral buffers and cached files
        cleanName in TEMP_DIR_NAMES -> {
            val zipFolderIcon = when (iconPack) {
                IconPackTheme.MINIMAL_ROUNDED -> Icons.Rounded.FolderZip
                IconPackTheme.OUTLINE -> Icons.Outlined.FolderZip
                IconPackTheme.SYSTEM_FILLED -> Icons.Filled.FolderZip
                IconPackTheme.CYBER_NEON -> Icons.Default.FolderZip
            }
            Triple(zipFolderIcon, null, Color(0xFFFB923C))
        }

        // Generic Folder
        else -> {
            Triple(genericFolderIcon, null, defaultFolderTint)
        }
    }

    val brush = brushForTint(tint)
    return FileVisualStyle(
        icon = folderIcon,
        tint = tint,
        containerBrush = brush,
        badgeText = null,
        folderEmblem = folderEmblem,
        isFolder = true
    )
}
