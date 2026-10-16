package com.codershubinc.aaxion_mob.feature.drive

import android.content.Intent
import androidx.compose.foundation.background
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codershubinc.aaxion_mob.core.model.FileItem
import com.codershubinc.aaxion_mob.core.network.buildThumbUrl
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.FileVisualIcon
import com.codershubinc.aaxion_mob.ui.designsystem.resolveFileStyle
import com.codershubinc.aaxion_mob.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileItem,
    baseUrl: String,
    token: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showActionModal by remember { mutableStateOf(false) }

    val thumbUrl = remember(baseUrl, file.rawPath, token) {
        file.buildThumbUrl(baseUrl, token)
    }

    val imageRequest = remember(thumbUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .size(128)
            .build()
    }

    val fileStyle = resolveFileStyle(file)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (onLongClick != null) {
                        onLongClick()
                    } else {
                        showActionModal = true
                    }
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Translucent obsidian glass tile with specular border and soft radial glow
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassSurfaceLow)
                .border(1.dp, AaxionBorder, RoundedCornerShape(12.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                fileStyle.tint.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 1.4f
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (file.isImage) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(11.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                FileVisualIcon(
                    fileStyle = fileStyle,
                    iconSize = 36.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // File name with middle ellipsis, preserving extension and max 2 lines
        Text(
            text = file.displayName(30),
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showActionModal) {
        FileActionModal(
            file = file,
            baseUrl = baseUrl,
            token = token,
            onDismiss = { showActionModal = false },
            onOpen = {
                showActionModal = false
                onClick()
            }
        )
    }
}
