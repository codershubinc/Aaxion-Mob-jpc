package com.codershubinc.aaxion_mob.feature.drive

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*

data class BreadcrumbSegment(
    val name: String,
    val fullPath: String,
    val isRoot: Boolean = false,
    val isExternal: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveBreadcrumbs(
    currentPath: String,
    itemCount: Int,
    accentColor: Color,
    onNavigate: (path: String) -> Unit,
    onNavigateUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPathDialog by remember { mutableStateOf(false) }
    var customPathInput by remember { mutableStateOf("") }

    val segments = remember(currentPath) {
        val list = mutableListOf<BreadcrumbSegment>()
        val isExt = currentPath.startsWith("/media") ||
                currentPath.startsWith("/mnt") ||
                currentPath.startsWith("/run/media") ||
                currentPath.startsWith("/storage") ||
                currentPath.contains("/external")

        // Root segment
        list.add(
            BreadcrumbSegment(
                name = if (isExt) "External" else "Root",
                fullPath = "/",
                isRoot = true,
                isExternal = isExt
            )
        )

        // Split non-empty path parts
        val parts = currentPath.split("/").filter { it.isNotEmpty() }
        var acc = ""
        for (i in parts.indices) {
            acc += "/${parts[i]}"
            list.add(
                BreadcrumbSegment(
                    name = parts[i],
                    fullPath = acc,
                    isRoot = false,
                    isExternal = isExt
                )
            )
        }
        list
    }

    val scrollState = rememberScrollState()

    // Auto-scroll to the end when path updates so user sees the current active folder
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val breadcrumbShape = RoundedCornerShape(8.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(breadcrumbShape)
            .background(GlassSurfaceLow)
            .border(1.dp, GlassBrushes.specularBorder(), breadcrumbShape)
            .combinedClickable(
                onClick = {},
                onLongClick = { 
                    customPathInput = currentPath
                    showPathDialog = true 
                }
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        val canGoUp = currentPath != "/" && currentPath.isNotEmpty() && onNavigateUp != null
        if (canGoUp) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.14f))
                    .clickable { onNavigateUp?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Up",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Scrollable crumbs row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            segments.forEachIndexed { index, segment ->
                val isLast = index == segments.size - 1

                if (segment.isRoot) {
                    // Root / Drive icon pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLast) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable(enabled = !isLast) { onNavigate(segment.fullPath) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (segment.isExternal) StreamerOrange.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (segment.isExternal) "EXT" else "SYS",
                                color = if (segment.isExternal) StreamerOrange else accentColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (segment.isExternal) Icons.Default.Usb else Icons.Default.Dns,
                            contentDescription = null,
                            tint = if (isLast) (if (segment.isExternal) StreamerOrange else accentColor) else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = segment.name,
                            color = if (isLast) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                } else {
                    // Folder segment
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLast) accentColor.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable(enabled = !isLast) { onNavigate(segment.fullPath) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = segment.name,
                            color = if (isLast) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Separator icon between segments
                if (!isLast) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(horizontal = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Item count pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF141414))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$itemCount",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showPathDialog) {
        GlassDialog(
            onDismissRequest = { showPathDialog = false },
            accentHighlight = accentColor
        ) {
            Text(
                text = "Go to Directory",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = customPathInput,
                onValueChange = { customPathInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = AaxionBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showPathDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    text = "GO",
                    onClick = {
                        showPathDialog = false
                        var targetPath = customPathInput.trim()
                        
                        val lastSlashIdx = targetPath.lastIndexOf('/')
                        val lastPart = if (lastSlashIdx != -1) targetPath.substring(lastSlashIdx + 1) else targetPath
                        
                        val dotIndex = lastPart.lastIndexOf('.')
                        val hasValidExtension = if (dotIndex > 0 && dotIndex < lastPart.length - 1) {
                            val ext = lastPart.substring(dotIndex + 1)
                            ext.length in 1..5 && ext.all { it.isLetterOrDigit() }
                        } else false

                        if (hasValidExtension && !targetPath.endsWith("/")) {
                            targetPath = if (lastSlashIdx != -1) targetPath.substring(0, lastSlashIdx) else "/"
                            if (targetPath.isEmpty()) targetPath = "/"
                            Toast.makeText(context, "File path detected. Navigating to parent directory...", Toast.LENGTH_SHORT).show()
                        }
                        
                        if (targetPath.isNotBlank()) {
                            onNavigate(targetPath)
                        }
                    },
                    accentColor = accentColor
                )
            }
        }
    }
}
