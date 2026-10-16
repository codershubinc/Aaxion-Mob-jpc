package com.codershubinc.aaxion_mob.feature.drive.viwerModals.code

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.network.buildDownloadUrl
import com.codershubinc.aaxion_mob.core.util.DownloadHelper
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.codershubinc.aaxion_mob.core.util.TextContentOptimizer
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Obsidian Dark Code Viewer modal with syntax highlighting, search/find,
 * word wrap, line numbers gutter, and copy actions.
 */
@Composable
fun CodeViewerModal(
    file: FileItem,
    serverUri: String,
    token: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    val language = remember(file.name, file.extension) {
        SupportedLanguage.fromFile(file.name, file.extension)
    }
    val deviconUrl = remember(file.name, file.extension) {
        DeviconHelper.getDeviconUrl(file.name, file.extension)
    }

    var codeContent by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var wrapLines by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch and optimize code content asynchronously
    LaunchedEffect(file.rawPath, serverUri, token) {
        isLoading = true
        errorMessage = null
        val downloadUrl = file.buildDownloadUrl(serverUri, token)

        try {
            val raw = withContext(Dispatchers.IO) {
                val client = Fetcher.getInstance().getClient()
                val request = Request.Builder()
                    .url(downloadUrl)
                    .build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val bodyBytes = response.body?.bytes()
                    if (bodyBytes != null) {
                        String(bodyBytes, StandardCharsets.UTF_8)
                    } else {
                        throw IllegalStateException("Empty response received.")
                    }
                } else {
                    throw IllegalStateException("Server error: HTTP ${response.code}")
                }
            }

            codeContent = raw
            // Auto-format minified JSON and chunk long lines to prevent UI thread lock
            val optimizedLines = TextContentOptimizer.optimize(
                rawText = raw,
                fileExtension = file.extension,
                fileName = file.name
            )
            lines = optimizedLines
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load code file."
        } finally {
            isLoading = false
        }
    }

    // Compute total matches asynchronously to ensure UI thread responsiveness
    val searchMatchCount by produceState(initialValue = 0, lines, searchQuery) {
        if (searchQuery.isBlank()) {
            value = 0
        } else {
            value = withContext(Dispatchers.Default) {
                val q = searchQuery.lowercase(Locale.ROOT)
                lines.sumOf { line ->
                    var count = 0
                    var idx = line.lowercase(Locale.ROOT).indexOf(q)
                    while (idx != -1) {
                        count++
                        idx = line.lowercase(Locale.ROOT).indexOf(q, idx + q.length)
                    }
                    count
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        SyncDialogWindow()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF09090D))
        ) {
            // Top Obsidian Navigation Surface
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassBrushes.glassSurface())
                    .border(0.75.dp, GlassHairline, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // File Title, Language Badge & Subtitle
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = file.displayName(24),
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Language Pill Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(language.badgeColor.copy(alpha = 0.20f))
                                        .border(0.6.dp, language.badgeColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (deviconUrl != null) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(deviconUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                imageLoader = DeviconHelper.getImageLoader(context),
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        Text(
                                            text = language.displayName,
                                            color = language.badgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (codeContent != null) {
                                    "${lines.size} lines • ${IntentUtils.formatFileSize(file.size)}"
                                } else {
                                    IntentUtils.formatFileSize(file.size)
                                },
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Action 1: Toggle Search / Find
                        IconButton(
                            onClick = {
                                showSearchBar = !showSearchBar
                                if (!showSearchBar) searchQuery = ""
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (showSearchBar) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Find in code",
                                tint = if (showSearchBar) accentColor else TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Action 2: Toggle Line Wrap
                        IconButton(
                            onClick = { wrapLines = !wrapLines },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (wrapLines) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.WrapText,
                                contentDescription = "Toggle Wrap",
                                tint = if (wrapLines) accentColor else TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Action 3: Copy Code
                        IconButton(
                            onClick = {
                                if (!codeContent.isNullOrEmpty()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Source Code", codeContent))
                                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = codeContent != null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = if (codeContent != null) TextSecondary else TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Action 4: Download File
                        IconButton(
                            onClick = {
                                val downloadUrl = file.buildDownloadUrl(serverUri, token)
                                DownloadHelper.enqueueDownload(context, downloadUrl, file.name, token)
                                Toast.makeText(context, "Download enqueued", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download File",
                                tint = accentColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Expandable Find / Search Bar
                    AnimatedVisibility(
                        visible = showSearchBar,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF14141E))
                                    .border(0.75.dp, GlassHairline, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    cursorBrush = SolidColor(accentColor),
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Find in file...",
                                                color = TextTertiary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (searchQuery.isNotEmpty()) {
                                    Text(
                                        text = if (searchMatchCount > 0) "$searchMatchCount matches" else "No matches",
                                        color = if (searchMatchCount > 0) accentColor else AaxionDanger,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Code Canvas Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CloudLoadingIcon(
                                    color = accentColor,
                                    modifier = Modifier.size(46.dp)
                                )
                                Text(
                                    text = "Highlighting ${file.extension.uppercase()} code...",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = AaxionDanger,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = errorMessage ?: "Failed to open code file",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        val downloadUrl = file.buildDownloadUrl(serverUri, token)
                                        DownloadHelper.enqueueDownload(context, downloadUrl, file.name, token)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("Download File", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    codeContent != null -> {
                        val lazyListState = rememberLazyListState()
                        val hScrollState = rememberScrollState()

                        val horizontalModifier = if (!wrapLines) {
                            Modifier.horizontalScroll(hScrollState)
                        } else {
                            Modifier
                        }

                        val gutterWidth = remember(lines.size) {
                            when {
                                lines.size >= 10000 -> 56.dp
                                lines.size >= 1000 -> 48.dp
                                else -> 40.dp
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(bottom = 36.dp)
                        ) {
                            itemsIndexed(
                                items = lines,
                                key = { index, _ -> index }
                            ) { index, lineText ->
                                val highlightedLine = remember(lineText, language, searchQuery) {
                                    highlightCodeLine(lineText, language, searchQuery)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(horizontalModifier)
                                        .padding(vertical = 1.5.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Line Number Gutter
                                    Text(
                                        text = "${index + 1}",
                                        color = TextTertiary,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier
                                            .width(gutterWidth)
                                            .padding(end = 12.dp)
                                    )

                                    // Hairline Gutter Divider
                                    Box(
                                        modifier = Modifier
                                            .width(0.75.dp)
                                            .height(18.dp)
                                            .background(GlassHairline)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Highlighted Code Line
                                    Text(
                                        text = highlightedLine,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp,
                                        softWrap = wrapLines,
                                        modifier = Modifier
                                            .weight(1f, fill = wrapLines)
                                            .padding(end = 16.dp)
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
