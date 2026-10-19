package com.codershubinc.aaxion_mob.feature.drive.viwerModals

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionToast
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionToastHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.codershubinc.aaxion_mob.core.util.TextContentOptimizer
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.nio.charset.StandardCharsets

/**
 * Built-in text and UTF code viewer modal.
 * Displays source code, markdown, logs, scripts, and plain text with line numbers,
 * optional word wrap, monospaced typography, clipboard copying, and file downloads.
 */
@Composable
fun TextViewerModal(
    file: FileItem,
    serverUri: String,
    token: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    val isMarkdown = remember(file.name, file.extension) {
        val ext = file.extension.lowercase(java.util.Locale.ROOT)
        val lowerName = file.name.lowercase(java.util.Locale.ROOT)
        ext in setOf("md", "markdown") || lowerName.startsWith("readme")
    }
    var isRawMode by remember { mutableStateOf(!isMarkdown) }
    var textContent by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wrapLines by remember { mutableStateOf(false) }

    // Fetch and optimize text content asynchronously
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
                        throw IllegalStateException("Empty file response received.")
                    }
                } else {
                    throw IllegalStateException("Server error: HTTP ${response.code}")
                }
            }

            textContent = raw
            lines = TextContentOptimizer.optimize(
                rawText = raw,
                fileExtension = file.extension,
                fileName = file.name
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load document content."
        } finally {
            isLoading = false
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF09090D))
            ) {
            // Obsidian Glass Top Navigation Bar
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
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // File Information
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = file.displayName(24),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Extension Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .border(0.6.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (file.extension.isNotEmpty()) file.extension.uppercase().take(6) else file.name.uppercase().take(6).ifEmpty { "TXT" },
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            val linesCount = textContent?.lines()?.size ?: 0
                            Text(
                                text = if (textContent != null) {
                                    "$linesCount lines • ${IntentUtils.formatFileSize(file.size)}"
                                } else {
                                    IntentUtils.formatFileSize(file.size)
                                },
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Action 0: Toggle Markdown Formatted vs Raw View
                        if (isMarkdown) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRawMode) Color.White.copy(alpha = 0.08f) else accentColor.copy(alpha = 0.20f))
                                    .border(0.75.dp, if (isRawMode) GlassHairline else accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .clickable { isRawMode = !isRawMode }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRawMode) Icons.Default.Visibility else Icons.Default.Code,
                                        contentDescription = if (isRawMode) "Preview Markdown" else "View Raw",
                                        tint = if (isRawMode) TextSecondary else accentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isRawMode) "Raw" else "Preview",
                                        color = if (isRawMode) TextSecondary else accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Action 1: Toggle Line Wrap (for Raw View)
                        if (isRawMode) {
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
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Action 2: Copy All Text
                        IconButton(
                            onClick = {
                                textContent?.let { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("File Content", text)
                                    clipboard.setPrimaryClip(clip)
                                    AaxionToast.show(context, "Copied content to clipboard")
                                }
                            },
                            enabled = textContent != null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = if (textContent != null) Color.White else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Action 3: Download File
                        IconButton(
                            onClick = {
                                val downloadUrl = file.buildDownloadUrl(serverUri, token)
                                DownloadHelper.enqueueDownload(context, downloadUrl, file.name, token)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download File",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Body: Content Area with Line Numbers Gutter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CloudLoadingIcon(
                                    color = accentColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Reading file content...",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = AaxionDanger,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = errorMessage ?: "Failed to open document",
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

                    textContent != null -> {
                        if (isMarkdown && !isRawMode) {
                            MarkdownDocumentView(
                                content = textContent!!,
                                modifier = Modifier.fillMaxSize(),
                                accentColor = accentColor
                            )
                        } else {
                            val lazyListState = rememberLazyListState()
                            val hScrollState = rememberScrollState()

                            val horizontalModifier = if (!wrapLines) {
                                Modifier.horizontalScroll(hScrollState)
                            } else {
                                Modifier
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp),
                                contentPadding = PaddingValues(bottom = 32.dp)
                            ) {
                                itemsIndexed(
                                    items = lines,
                                    key = { index, _ -> index }
                                ) { index, lineText ->
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
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier
                                                .width(42.dp)
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

                                        // Code / Text Line
                                        Text(
                                            text = if (lineText.isEmpty()) " " else lineText,
                                            color = TextPrimary,
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
            AaxionToastHost()
        }
    }
}
}
