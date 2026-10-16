package com.codershubinc.aaxion_mob.feature.drive.viwerModals

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Built-in PDF document viewer modal.
 * Uses hardware-accelerated document page rendering, multi-page lazy scrolling,
 * pinch-to-zoom, page progress indicators, and glassmorphic navigation.
 */
@Composable
fun PdfViewerModal(
    file: FileItem,
    serverUri: String,
    token: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Download and initialize PDF renderer
    LaunchedEffect(file.rawPath, serverUri, token) {
        isLoading = true
        errorMessage = null

        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "preview_pdf_${file.name.hashCode()}.pdf")
                val downloadUrl = file.buildDownloadUrl(serverUri, token)

                if (!tempFile.exists() || tempFile.length() == 0L) {
                    val client = Fetcher.getInstance().getClient()
                    val request = Request.Builder().url(downloadUrl).build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful && response.body != null) {
                        FileOutputStream(tempFile).use { output ->
                            response.body!!.byteStream().copyTo(output)
                        }
                    } else {
                        throw IllegalStateException("Failed to download document: HTTP ${response.code}")
                    }
                }

                val descriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)

                pfd = descriptor
                pdfRenderer = renderer
                pageCount = renderer.pageCount
            } catch (e: SecurityException) {
                errorMessage = "This PDF document is password-protected."
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to open PDF document."
            } finally {
                isLoading = false
            }
        }
    }

    // Clean up native resources on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C0C10))
        ) {
            val lazyListState = rememberLazyListState()

            // Main PDF Page Viewer
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
                                text = "Preparing PDF document...",
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AaxionDanger,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = errorMessage ?: "Unable to render PDF",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val downloadUrl = file.buildDownloadUrl(serverUri, token)
                                    IntentUtils.launchViewIntent(context, downloadUrl, "application/pdf")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Open in External Viewer", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                pdfRenderer != null && pageCount > 0 -> {
                    val renderer = pdfRenderer!!

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.0f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        offset += pan
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 80.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(
                                count = pageCount,
                                key = { it }
                            ) { pageIndex ->
                                PdfPageCard(
                                    renderer = renderer,
                                    pageIndex = pageIndex,
                                    pageCount = pageCount
                                )
                            }
                        }
                    }
                }
            }

            // Top Scrim & Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.90f), Color.Transparent)
                        )
                    )
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
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Page Indicator
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.displayName(26),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (pageCount > 0) {
                            val currentPage = (lazyListState.firstVisibleItemIndex + 1).coerceAtMost(pageCount)
                            Text(
                                text = "Page $currentPage of $pageCount • ${IntentUtils.formatFileSize(file.size)}",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = IntentUtils.formatFileSize(file.size),
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Reset Zoom Button
                    AnimatedVisibility(
                        visible = scale > 1.05f,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = accentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Download Action
                    IconButton(
                        onClick = {
                            val downloadUrl = file.buildDownloadUrl(serverUri, token)
                            DownloadHelper.enqueueDownload(context, downloadUrl, file.name, token)
                            Toast.makeText(context, "Downloading ${file.name}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Document",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Bottom Floating Page Capsule
            if (pageCount > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val currentPage = (lazyListState.firstVisibleItemIndex + 1).coerceAtMost(pageCount)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(0.8.dp, GlassHairline, RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Page $currentPage / $pageCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual PDF page card rendered to bitmap on demand.
 */
@Composable
private fun PdfPageCard(
    renderer: PdfRenderer,
    pageIndex: Int,
    pageCount: Int
) {
    var pageBitmap by remember(pageIndex, renderer) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, renderer) {
        withContext(Dispatchers.Default) {
            try {
                synchronized(renderer) {
                    val page = renderer.openPage(pageIndex)
                    // High-density scaling for crisp text rendering
                    val targetWidth = (page.width * 2).coerceIn(600, 1800)
                    val targetHeight = (targetWidth * page.height / page.width)

                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(AndroidColor.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pageBitmap = bmp
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1} of $pageCount",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f) // Standard ISO 216 A4 aspect ratio
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
