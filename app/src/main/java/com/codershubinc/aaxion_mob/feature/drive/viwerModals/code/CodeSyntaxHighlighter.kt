package com.codershubinc.aaxion_mob.feature.drive.viwerModals.code

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.Locale

/**
 * Obsidian Dark Syntax Highlighting Color Palette.
 */
object CodePalette {
    val Plain = Color(0xFFE2E8F0)
    val Keyword = Color(0xFFC792EA)        // Soft Violet / Purple
    val StringLiteral = Color(0xFFC3E88D)  // Neon Sage / Green
    val NumberLiteral = Color(0xFFF78C6C)  // Warm Coral / Orange
    val Comment = Color(0xFF676E95)        // Muted Indigo / Slate
    val Function = Color(0xFF82AAFF)       // Sky Blue
    val TypeOrClass = Color(0xFFFFCB6B)    // Amber / Gold
    val Annotation = Color(0xFFFF5370)     // Neon Rose / Coral
    val Operator = Color(0xFF89DDFF)       // Light Cyan
    val Punctuation = Color(0xFF89DDFF)    // Cyan / Slate
    val PropertyKey = Color(0xFF7FDBCA)    // Teal / Mint
    val Tag = Color(0xFFFF5370)            // Markup Tag Rose
    val Attribute = Color(0xFF7FDBCA)      // Markup Attribute Teal
    val SearchHighlightBg = Color(0x66FFD600) // Vibrant yellow highlight
}

/**
 * Language classifications for source files.
 */
enum class SupportedLanguage(val displayName: String, val badgeColor: Color) {
    TYPESCRIPT("TS", Color(0xFF3178C6)),
    JAVASCRIPT("JS", Color(0xFFE5A00D)),
    KOTLIN("KT", Color(0xFF7F52FF)),
    JAVA("JAVA", Color(0xFFED8B00)),
    PYTHON("PY", Color(0xFF3776AB)),
    RUST("RS", Color(0xFFDEA584)),
    GO("GO", Color(0xFF00ADD8)),
    CPP("C++", Color(0xFF00599C)),
    CSHARP("C#", Color(0xFF239120)),
    HTML("HTML", Color(0xFFE34F26)),
    CSS("CSS", Color(0xFF1572B6)),
    JSON("JSON", Color(0xFF4CAF50)),
    SQL("SQL", Color(0xFFE38C00)),
    SHELL("SH", Color(0xFF4EAA25)),
    YAML("YAML", Color(0xFFCB171E)),
    XML("XML", Color(0xFF005F9E)),
    GENERIC("CODE", Color(0xFF818CF8));

    companion object {
        fun fromFile(name: String, ext: String): SupportedLanguage {
            val lowerName = name.lowercase(Locale.ROOT)
            val lowerExt = ext.lowercase(Locale.ROOT)

            return when {
                lowerName in setOf("makefile", "gnumakefile") -> SHELL
                lowerName in setOf("dockerfile", "containerfile") -> SHELL
                lowerName.startsWith(".git") -> SHELL
                lowerName.startsWith(".env") -> SHELL
                lowerName.endsWith("rc") || lowerName.endsWith("profile") -> SHELL
                lowerName in setOf("gemfile", "rakefile") -> GENERIC
                lowerName == "cmakelists.txt" -> CPP
                else -> fromExtension(lowerExt)
            }
        }

        fun fromExtension(ext: String): SupportedLanguage {
            return when (ext.lowercase(Locale.ROOT)) {
                "ts", "tsx" -> TYPESCRIPT
                "js", "jsx", "mjs", "cjs" -> JAVASCRIPT
                "kt", "kts" -> KOTLIN
                "java" -> JAVA
                "py", "pyw" -> PYTHON
                "rs" -> RUST
                "go" -> GO
                "c", "cpp", "cc", "cxx", "h", "hpp" -> CPP
                "cs" -> CSHARP
                "html", "htm" -> HTML
                "css", "scss", "sass", "less" -> CSS
                "json" -> JSON
                "sql" -> SQL
                "sh", "bash", "zsh" -> SHELL
                "yaml", "yml" -> YAML
                "xml", "svg" -> XML
                else -> GENERIC
            }
        }
    }
}

private val TS_KEYWORDS = setOf(
    "abstract", "any", "as", "asserts", "async", "await", "bigint", "boolean", "break",
    "case", "catch", "class", "const", "constructor", "continue", "debugger", "declare",
    "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally",
    "for", "from", "function", "get", "if", "implements", "import", "in", "infer",
    "instanceof", "interface", "is", "keyof", "let", "module", "namespace", "never",
    "new", "null", "number", "object", "of", "package", "private", "protected", "public",
    "readonly", "require", "return", "set", "static", "string", "super", "switch",
    "symbol", "this", "throw", "true", "try", "type", "typeof", "undefined", "unknown",
    "var", "void", "while", "with", "yield"
)

private val KOTLIN_KEYWORDS = setOf(
    "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
    "companion", "const", "constructor", "continue", "crossinline", "data", "delegate",
    "do", "dynamic", "else", "enum", "expect", "external", "false", "field", "final",
    "finally", "for", "fun", "get", "if", "import", "in", "infix", "init", "inline",
    "inner", "interface", "internal", "is", "it", "lateinit", "noinline", "null",
    "object", "open", "operator", "out", "override", "package", "param", "private",
    "protected", "public", "reified", "return", "sealed", "set", "super", "suspend",
    "tailrec", "this", "throw", "true", "try", "typealias", "val", "var", "vararg",
    "when", "where", "while"
)

private val PYTHON_KEYWORDS = setOf(
    "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
    "del", "elif", "else", "except", "False", "finally", "for", "from", "global",
    "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass",
    "raise", "return", "True", "try", "while", "with", "yield", "self", "cls"
)

private val RUST_KEYWORDS = setOf(
    "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else",
    "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop",
    "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static",
    "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while"
)

private val GO_KEYWORDS = setOf(
    "break", "case", "chan", "const", "continue", "default", "defer", "else",
    "fallthrough", "for", "func", "go", "goto", "if", "import", "interface", "map",
    "package", "range", "return", "select", "struct", "switch", "type", "var",
    "true", "false", "nil", "iota", "make", "new", "len", "cap", "append"
)

private val SQL_KEYWORDS = setOf(
    "select", "from", "where", "insert", "into", "update", "delete", "create",
    "table", "drop", "alter", "add", "join", "inner", "left", "right", "outer",
    "full", "on", "group", "by", "order", "having", "limit", "offset", "distinct",
    "as", "and", "or", "not", "null", "is", "in", "between", "like", "case",
    "when", "then", "else", "end", "primary", "key", "foreign", "references",
    "values", "set", "count", "sum", "avg", "min", "max", "exists", "union",
    "all", "view", "index", "database", "schema"
)

private val CPP_KEYWORDS = setOf(
    "auto", "bool", "break", "case", "catch", "char", "class", "const", "constexpr",
    "continue", "default", "delete", "do", "double", "else", "enum", "explicit",
    "export", "extern", "false", "float", "for", "friend", "goto", "if", "inline",
    "int", "long", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
    "private", "protected", "public", "register", "return", "short", "signed",
    "sizeof", "static", "struct", "switch", "template", "this", "throw", "true",
    "try", "typedef", "typename", "union", "unsigned", "using", "virtual", "void",
    "volatile", "while"
)

private val GENERIC_KEYWORDS = TS_KEYWORDS + KOTLIN_KEYWORDS + PYTHON_KEYWORDS

/**
 * Highlights a single line of source code into an [AnnotatedString].
 * Memoized and tokenized efficiently for instant scrolling in LazyColumn.
 */
fun highlightCodeLine(
    line: String,
    language: SupportedLanguage,
    searchQuery: String = ""
): AnnotatedString {
    if (line.isEmpty()) {
        return buildAnnotatedString { append(" ") }
    }

    // Safety guard: For unusually long lines, syntax-highlight only up to 800 characters
    // and append remainder as plain text to guarantee UI thread responsiveness.
    val maxHighlightChars = 800
    val (targetLine, suffix) = if (line.length > maxHighlightChars) {
        line.substring(0, maxHighlightChars) to line.substring(maxHighlightChars)
    } else {
        line to null
    }

    val trimmed = targetLine.trimStart()
    val leadingSpacesCount = targetLine.length - trimmed.length

    // 1. Fast Full-Line Comment Checks
    val isFullLineComment = when (language) {
        SupportedLanguage.PYTHON, SupportedLanguage.SHELL, SupportedLanguage.YAML ->
            trimmed.startsWith("#")
        SupportedLanguage.SQL ->
            trimmed.startsWith("--")
        SupportedLanguage.HTML, SupportedLanguage.XML ->
            trimmed.startsWith("<!--")
        else ->
            trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")
    }

    if (isFullLineComment) {
        val commentAnnotated = buildAnnotatedString {
            withStyle(SpanStyle(color = CodePalette.Comment, fontStyle = FontStyle.Italic)) {
                append(line)
            }
        }
        return applySearchHighlight(commentAnnotated, searchQuery)
    }

    // 2. Tokenizer for Line
    val baseAnnotated = buildAnnotatedString {
        if (leadingSpacesCount > 0) {
            append(targetLine.take(leadingSpacesCount))
        }

        var i = leadingSpacesCount
        val len = targetLine.length

        while (i < len) {
            val c = line[i]

            // Trailing Line Comment
            val hasTrailingComment = when (language) {
                SupportedLanguage.PYTHON, SupportedLanguage.SHELL, SupportedLanguage.YAML ->
                    c == '#'
                SupportedLanguage.SQL ->
                    c == '-' && i + 1 < len && line[i + 1] == '-'
                else ->
                    c == '/' && i + 1 < len && (line[i + 1] == '/' || line[i + 1] == '*')
            }

            if (hasTrailingComment) {
                withStyle(SpanStyle(color = CodePalette.Comment, fontStyle = FontStyle.Italic)) {
                    append(line.substring(i))
                }
                break
            }

            // String Literals
            if (c == '"' || c == '\'' || c == '`') {
                val quote = c
                var end = i + 1
                while (end < len) {
                    if (line[end] == '\\') {
                        end += 2
                        continue
                    }
                    if (line[end] == quote) {
                        end++
                        break
                    }
                    end++
                }
                val strContent = line.substring(i, minOf(end, len))

                // In JSON, check if followed by colon (Property Key)
                val isJsonKey = language == SupportedLanguage.JSON && run {
                    var k = end
                    while (k < len && line[k].isWhitespace()) k++
                    k < len && line[k] == ':'
                }

                val strColor = if (isJsonKey) CodePalette.PropertyKey else CodePalette.StringLiteral
                withStyle(SpanStyle(color = strColor)) {
                    append(strContent)
                }
                i = end
                continue
            }

            // Numeric Literals
            if (c.isDigit() || (c == '.' && i + 1 < len && line[i + 1].isDigit())) {
                val start = i
                if (c == '0' && i + 1 < len && (line[i + 1] == 'x' || line[i + 1] == 'X')) {
                    i += 2
                    while (i < len && (line[i].isDigit() || line[i].lowercaseChar() in 'a'..'f')) i++
                } else {
                    while (i < len && (line[i].isDigit() || line[i] == '.' || line[i] == '_' || line[i].lowercaseChar() in setOf('f', 'l', 'u', 'e', 'b'))) i++
                }
                withStyle(SpanStyle(color = CodePalette.NumberLiteral)) {
                    append(line.substring(start, i))
                }
                continue
            }

            // Annotations / Decorators
            if (c == '@' && i + 1 < len && (line[i + 1].isLetter() || line[i + 1] == '_')) {
                val start = i
                i++
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '.')) i++
                withStyle(SpanStyle(color = CodePalette.Annotation, fontWeight = FontWeight.SemiBold)) {
                    append(line.substring(start, i))
                }
                continue
            }

            // Identifiers / Keywords / Types
            if (c.isLetter() || c == '_' || c == '$') {
                val start = i
                while (i < len && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '$')) i++
                val word = line.substring(start, i)

                val keywords = when (language) {
                    SupportedLanguage.TYPESCRIPT, SupportedLanguage.JAVASCRIPT -> TS_KEYWORDS
                    SupportedLanguage.KOTLIN, SupportedLanguage.JAVA -> KOTLIN_KEYWORDS
                    SupportedLanguage.PYTHON -> PYTHON_KEYWORDS
                    SupportedLanguage.RUST -> RUST_KEYWORDS
                    SupportedLanguage.GO -> GO_KEYWORDS
                    SupportedLanguage.CPP, SupportedLanguage.CSHARP -> CPP_KEYWORDS
                    SupportedLanguage.SQL -> SQL_KEYWORDS
                    else -> GENERIC_KEYWORDS
                }

                val isKeyword = if (language == SupportedLanguage.SQL) {
                    keywords.contains(word.lowercase(Locale.ROOT))
                } else {
                    keywords.contains(word)
                }

                // Check if followed by '(' -> function call
                var lookAhead = i
                while (lookAhead < len && line[lookAhead].isWhitespace()) lookAhead++
                val isFunction = lookAhead < len && line[lookAhead] == '('

                val isType = !isKeyword && word.first().isUpperCase() && word.any { it.isLowerCase() }

                val tokenColor = when {
                    isKeyword -> CodePalette.Keyword
                    isFunction -> CodePalette.Function
                    isType -> CodePalette.TypeOrClass
                    else -> CodePalette.Plain
                }

                val fontWeight = if (isKeyword) FontWeight.SemiBold else FontWeight.Normal

                withStyle(SpanStyle(color = tokenColor, fontWeight = fontWeight)) {
                    append(word)
                }
                continue
            }

            // Operators & Punctuation
            val isOperator = c in setOf('=', '+', '-', '*', '/', '%', '!', '<', '>', '&', '|', '^', '?', ':')
            val isPunctuation = c in setOf('{', '}', '(', ')', '[', ']', ';', ',')

            if (isOperator) {
                withStyle(SpanStyle(color = CodePalette.Operator)) {
                    append(c)
                }
            } else if (isPunctuation) {
                withStyle(SpanStyle(color = CodePalette.Punctuation)) {
                    append(c)
                }
            } else {
                append(c)
            }
            i++
        }
        if (suffix != null) {
            append(suffix)
        }
    }

    return applySearchHighlight(baseAnnotated, searchQuery)
}

/**
 * Applies search match background spans to an already highlighted [AnnotatedString].
 */
private fun applySearchHighlight(annotated: AnnotatedString, query: String): AnnotatedString {
    if (query.isBlank()) return annotated

    val text = annotated.text
    val queryLower = query.lowercase(Locale.ROOT)
    val textLower = text.lowercase(Locale.ROOT)

    var matchIndex = textLower.indexOf(queryLower)
    if (matchIndex == -1) return annotated

    return buildAnnotatedString {
        append(annotated)
        while (matchIndex != -1) {
            addStyle(
                style = SpanStyle(
                    background = CodePalette.SearchHighlightBg,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                start = matchIndex,
                end = matchIndex + query.length
            )
            matchIndex = textLower.indexOf(queryLower, matchIndex + query.length)
        }
    }
}
