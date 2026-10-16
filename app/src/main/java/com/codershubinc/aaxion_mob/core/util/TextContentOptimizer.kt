package com.codershubinc.aaxion_mob.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

/**
 * Optimizes text and code content for performant virtualization in UI lists.
 * Prevents UI freezes and thread lockups on large single-line documents,
 * minified files, or giant data structures.
 */
object TextContentOptimizer {

    private const val MAX_SAFE_LINE_LENGTH = 800
    private const val MAX_FORMAT_FILE_SIZE = 2_000_000 // 2 MB limit for auto-formatting

    /**
     * Prepares raw document content for display by auto-formatting minified structures
     * and chunking overly long single lines to guarantee smooth frame rendering.
     */
    suspend fun optimize(
        rawText: String,
        fileExtension: String = "",
        fileName: String = ""
    ): List<String> = withContext(Dispatchers.Default) {
        if (rawText.isEmpty()) return@withContext emptyList()

        val ext = fileExtension.lowercase(Locale.ROOT)
        val name = fileName.lowercase(Locale.ROOT)
        val isJson = ext == "json" || name.endsWith(".json") || (rawText.length in 2..MAX_FORMAT_FILE_SIZE && looksLikeJson(rawText))

        val processedText = if (isJson && isMinified(rawText)) {
            tryFormatJson(rawText) ?: rawText
        } else {
            rawText
        }

        sanitizeLines(processedText, MAX_SAFE_LINE_LENGTH)
    }

    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.startsWith('{') || trimmed.startsWith('[')
    }

    private fun isMinified(text: String): Boolean {
        if (text.length < 200) return false
        val firstNewline = text.indexOf('\n')
        if (firstNewline == -1) return true
        if (firstNewline > 500) return true
        val lineCount = text.count { it == '\n' } + 1
        val avgLineLength = text.length / lineCount
        return avgLineLength > 150
    }

    private fun tryFormatJson(raw: String): String? {
        if (raw.length > MAX_FORMAT_FILE_SIZE) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) return null

        return try {
            val tokener = JSONTokener(trimmed)
            val value = tokener.nextValue()
            when (value) {
                is JSONObject -> value.toString(2)
                is JSONArray -> value.toString(2)
                else -> formatJsonStreaming(trimmed)
            }
        } catch (_: Exception) {
            try {
                formatJsonStreaming(trimmed)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun formatJsonStreaming(json: String, indentSpaces: Int = 2): String {
        val indentStr = " ".repeat(indentSpaces)
        val sb = StringBuilder(json.length + (json.length / 4).coerceAtLeast(64))
        var indentLevel = 0
        var inString = false
        var isEscaped = false
        var i = 0
        val len = json.length

        fun appendIndent() {
            sb.append('\n')
            for (k in 0 until indentLevel) {
                sb.append(indentStr)
            }
        }

        while (i < len) {
            val c = json[i]
            if (inString) {
                sb.append(c)
                if (isEscaped) {
                    isEscaped = false
                } else if (c == '\\') {
                    isEscaped = true
                } else if (c == '"') {
                    inString = false
                }
            } else {
                when (c) {
                    '"' -> {
                        inString = true
                        sb.append(c)
                    }
                    '{', '[' -> {
                        sb.append(c)
                        var nextNonWs = i + 1
                        while (nextNonWs < len && json[nextNonWs].isWhitespace()) nextNonWs++
                        val matchingClose = if (c == '{') '}' else ']'
                        if (nextNonWs < len && json[nextNonWs] == matchingClose) {
                            sb.append(matchingClose)
                            i = nextNonWs
                        } else {
                            indentLevel++
                            appendIndent()
                        }
                    }
                    '}', ']' -> {
                        indentLevel = (indentLevel - 1).coerceAtLeast(0)
                        appendIndent()
                        sb.append(c)
                    }
                    ',' -> {
                        sb.append(c)
                        appendIndent()
                    }
                    ':' -> {
                        sb.append(": ")
                    }
                    ' ', '\t', '\r', '\n' -> {
                        // Skip unquoted whitespace
                    }
                    else -> {
                        sb.append(c)
                    }
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun sanitizeLines(text: String, maxLineLength: Int): List<String> {
        val rawLines = text.lines()
        val result = ArrayList<String>(rawLines.size)

        for (line in rawLines) {
            if (line.length <= maxLineLength) {
                result.add(line)
            } else {
                var start = 0
                while (start < line.length) {
                    val end = minOf(start + maxLineLength, line.length)
                    result.add(line.substring(start, end))
                    start = end
                }
            }
        }
        return result
    }
}
