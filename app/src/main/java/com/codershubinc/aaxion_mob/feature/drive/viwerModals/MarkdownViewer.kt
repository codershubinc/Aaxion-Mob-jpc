package com.codershubinc.aaxion_mob.feature.drive.viwerModals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionToast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.theme.*

/**
 * Structural representations of Markdown AST blocks.
 */
sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class BulletItem(val level: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    object Divider : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

/**
 * Parses inline formatting tags (**bold**, *italic*, `code`, ~~strike~~, [links](url))
 * into Compose [AnnotatedString] with theme-aware stylings.
 */
fun parseInlineMarkdown(text: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            // Bold: **text**
            if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Inline Code: `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = accentColor,
                            background = Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }

            // Strikethrough: ~~text~~
            if (i + 1 < text.length && text[i] == '~' && text[i + 1] == '~') {
                val end = text.indexOf("~~", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = TextTertiary)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Italic: *text*
            if (text[i] == '*') {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextSecondary)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            // Link: [title](url)
            if (text[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen != -1) {
                        val title = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(
                            SpanStyle(
                                color = accentColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(title)
                        }
                        pop()
                        i = closeParen + 1
                        continue
                    }
                }
            }

            append(text[i])
            i++
        }
    }
}

/**
 * Parses markdown lines into structured [MarkdownBlock] elements.
 */
fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val lines = content.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    var inCodeBlock = false
    var codeLang: String? = null
    val codeBuffer = mutableListOf<String>()

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (inCodeBlock) {
            if (trimmed.startsWith("```")) {
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.joinToString("\n")))
                inCodeBlock = false
                codeBuffer.clear()
                codeLang = null
            } else {
                codeBuffer.add(line)
            }
            i++
            continue
        }

        if (trimmed.startsWith("```")) {
            inCodeBlock = true
            codeLang = trimmed.removePrefix("```").trim().ifEmpty { null }
            codeBuffer.clear()
            i++
            continue
        }

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        when {
            // Headers
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                val text = trimmed.drop(level).trim()
                blocks.add(MarkdownBlock.Header(level, text))
            }

            // Horizontal Dividers
            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                blocks.add(MarkdownBlock.Divider)
            }

            // Blockquotes
            trimmed.startsWith(">") -> {
                val quoteText = trimmed.removePrefix(">").trim()
                blocks.add(MarkdownBlock.Blockquote(quoteText))
            }

            // Bullet Lists
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                val indentLevel = (line.takeWhile { it == ' ' }.length / 2).coerceAtMost(4)
                blocks.add(MarkdownBlock.BulletItem(indentLevel, trimmed.drop(2).trim()))
            }

            // Numbered Lists
            trimmed.firstOrNull()?.isDigit() == true && (trimmed.contains(". ") || trimmed.contains(") ")) -> {
                val num = trimmed.takeWhile { it.isDigit() }
                val rest = if (trimmed.contains(". ")) trimmed.substringAfter(". ") else trimmed.substringAfter(") ")
                blocks.add(MarkdownBlock.NumberedItem(num, rest.trim()))
            }

            // Tables
            trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().startsWith("|") && lines[i + 1].contains("---") -> {
                val rawHeaders = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                i += 2 // Skip header and separator row
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    val rowCells = lines[i].trim().split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    rows.add(rowCells)
                    i++
                }
                blocks.add(MarkdownBlock.Table(rawHeaders, rows))
                continue
            }

            // Standard Paragraph
            else -> {
                blocks.add(MarkdownBlock.Paragraph(line))
            }
        }

        i++
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.joinToString("\n")))
    }

    return blocks
}

/**
 * Formatted Markdown viewer canvas rendering headings, code blocks, lists,
 * blockquotes, tables, and rich text with obsidian glass aesthetics.
 */
@Composable
fun MarkdownDocumentView(
    content: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalAccentColor.current
) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(blocks) { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = if (block.level == 1) 8.dp else 4.dp)) {
                        val (fontSize, fontWeight) = when (block.level) {
                            1 -> 22.sp to FontWeight.Bold
                            2 -> 18.sp to FontWeight.Bold
                            3 -> 15.sp to FontWeight.SemiBold
                            else -> 13.5.sp to FontWeight.SemiBold
                        }
                        Text(
                            text = parseInlineMarkdown(block.text, accentColor),
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = Color.White
                        )
                        if (block.level == 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = GlassHairline, thickness = 0.8.dp)
                        }
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text, accentColor),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    val context = LocalContext.current
                    val codeShape = RoundedCornerShape(10.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(codeShape)
                            .background(Color(0xFF101015))
                            .border(0.75.dp, GlassHairline, codeShape)
                    ) {
                        // Code Header Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.language?.uppercase() ?: "CODE",
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", block.code))
                                    AaxionToast.show(context, "Code copied")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Code Body
                        val hScroll = rememberScrollState()
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(hScroll)
                                .padding(12.dp)
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .background(accentColor, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = parseInlineMarkdown(block.text, accentColor),
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextSecondary
                        )
                    }
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.level * 14).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp, end = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, accentColor),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextPrimary
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, accentColor),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextPrimary
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = GlassHairline,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                is MarkdownBlock.Table -> {
                    val tableScroll = rememberScrollState()
                    val tableShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(tableShape)
                            .border(0.75.dp, GlassHairline, tableShape)
                            .horizontalScroll(tableScroll)
                    ) {
                        Column {
                            // Headers
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(vertical = 8.dp)
                            ) {
                                block.headers.forEach { h ->
                                    Text(
                                        text = h,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.widthIn(min = 100.dp).padding(horizontal = 10.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = GlassHairline, thickness = 0.75.dp)

                            // Rows
                            block.rows.forEachIndexed { rIdx, row ->
                                Row(
                                    modifier = Modifier
                                        .background(if (rIdx % 2 == 1) Color.White.copy(alpha = 0.02f) else Color.Transparent)
                                        .padding(vertical = 7.dp)
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = parseInlineMarkdown(cell, accentColor),
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.widthIn(min = 100.dp).padding(horizontal = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
