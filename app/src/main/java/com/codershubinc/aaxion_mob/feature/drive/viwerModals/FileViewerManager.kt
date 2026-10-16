package com.codershubinc.aaxion_mob.feature.drive.viwerModals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.buildImageViewUrl
import com.codershubinc.aaxion_mob.feature.drive.viwerModals.code.CodeViewerModal
import java.util.Locale

/**
 * Built-in viewer types supported natively within the application.
 */
enum class FileViewerType {
    IMAGE,
    TEXT,
    CODE,
    PDF,
    NONE
}

/**
 * Plain text, Markdown, and log document file extensions.
 */
private val TEXT_VIEWER_EXTS = setOf(
    "txt", "md", "markdown", "log", "rtf", "csv", "tsv"
)

/**
 * Code, script, markup, schema, and config file extensions.
 */
private val CODE_VIEWER_EXTS = setOf(
    "ts", "tsx", "js", "jsx", "mjs", "cjs", "json",
    "kt", "kts", "java", "py", "pyw", "go", "rs",
    "c", "cpp", "cc", "cxx", "h", "hpp", "cs",
    "html", "htm", "css", "scss", "sass", "less",
    "xml", "svg", "yaml", "yml", "toml", "sql",
    "sh", "bash", "zsh", "php", "rb", "swift", "dart",
    "lua", "r", "lock", "dockerfile", "makefile", "env",
    "ini", "cfg", "conf", "properties", "gradle"
)
private val TEXT_VIEWER_FILENAMES = setOf(
    "license", "licence", "readme", "changelog", "authors", "contributors",
    "copying", "notice", "patents", "todo", "version"
)

private val CODE_VIEWER_FILENAMES = setOf(
    "makefile", "gnumakefile", "dockerfile", "containerfile", "gemfile",
    "procfile", "vagrantfile", "rakefile", "brewfile", "cmakelists.txt",
    ".gitignore", ".gitattributes", ".gitmodules", ".env", ".editorconfig",
    ".prettierrc", ".eslintrc", ".npmrc", ".dockerignore", ".bashrc",
    ".zshrc", ".profile", ".bash_profile", ".bash_aliases", ".zprofile"
)

/**
 * Resolves the appropriate in-app viewer mode for the given [file].
 */
fun resolveFileViewerType(file: FileItem): FileViewerType {
    if (file.isDir) return FileViewerType.NONE
    val ext = file.extension.lowercase(Locale.ROOT)
    val lowerName = file.name.lowercase(Locale.ROOT)
    val isDotCode = lowerName.startsWith('.') && (lowerName.endsWith("rc") || lowerName.endsWith("profile") || lowerName in CODE_VIEWER_FILENAMES)

    return when {
        file.isImage -> FileViewerType.IMAGE
        ext == "pdf" -> FileViewerType.PDF
        ext in CODE_VIEWER_EXTS || lowerName in CODE_VIEWER_FILENAMES || isDotCode -> FileViewerType.CODE
        ext in TEXT_VIEWER_EXTS || lowerName in TEXT_VIEWER_FILENAMES -> FileViewerType.TEXT
        else -> FileViewerType.NONE
    }
}

/**
 * Checks if the file can be opened directly within the application's built-in viewers.
 */
fun canOpenFileInViewer(file: FileItem): Boolean =
    resolveFileViewerType(file) != FileViewerType.NONE

/**
 * Centralized manager composable for all built-in file viewers.
 * Evaluates the file type and coordinates rendering the appropriate modal.
 */
@Composable
fun FileViewerModal(
    file: FileItem?,
    serverUri: String,
    token: String,
    onDismiss: () -> Unit
) {
    if (file == null) return

    val viewerType = remember(file.rawPath) { resolveFileViewerType(file) }

    when (viewerType) {
        FileViewerType.IMAGE -> {
            val imageUrl = remember(file.rawPath, serverUri, token) {
                file.buildImageViewUrl(serverUri, token)
            }
            ImageViewerModal(
                file = file,
                imageUrl = imageUrl,
                serverUri = serverUri,
                token = token,
                onDismiss = onDismiss
            )
        }

        FileViewerType.CODE -> {
            CodeViewerModal(
                file = file,
                serverUri = serverUri,
                token = token,
                onDismiss = onDismiss
            )
        }

        FileViewerType.TEXT -> {
            TextViewerModal(
                file = file,
                serverUri = serverUri,
                token = token,
                onDismiss = onDismiss
            )
        }

        FileViewerType.PDF -> {
            PdfViewerModal(
                file = file,
                serverUri = serverUri,
                token = token,
                onDismiss = onDismiss
            )
        }

        FileViewerType.NONE -> {
            // No built-in viewer available
        }
    }
}
