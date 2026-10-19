package com.codershubinc.aaxion_mob.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.media.DirectoryMediaType
import com.codershubinc.aaxion_mob.core.media.MediaDirectoryManager
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.feature.home.MarkedDirectoryTile
import com.codershubinc.aaxion_mob.ui.designsystem.InsetGroupedGlassCard
import com.codershubinc.aaxion_mob.ui.theme.GlassHairline
import com.codershubinc.aaxion_mob.ui.theme.TextPrimary
import com.codershubinc.aaxion_mob.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MarkedDirectoriesSection(
    serverUri: String,
    token: String,
    isServerOnline: Boolean?,
    refreshTrigger: Int,
    accentColor: Color,
    onNavigateToDrive: (path: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }
    val mediaDirManager = remember { MediaDirectoryManager.getInstance(context) }
    val markedDirMap by mediaDirManager.directoryTypes.collectAsState()
    val markedDirs = remember(markedDirMap) {
        markedDirMap.filter { it.value != DirectoryMediaType.NONE }
    }

    var dirAvailability by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var dirItemCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(serverUri, token, markedDirs, refreshTrigger, isServerOnline) {
        if (serverUri.isNotBlank() && token.isNotBlank() && isServerOnline != false) {
            withContext(Dispatchers.IO) {
                val service = Fetcher.getInstance().getService(serverUri)
                val newAvail = mutableMapOf<String, Boolean>()
                val newCounts = mutableMapOf<String, Int>()

                for ((path, _) in markedDirs) {
                    try {
                        val response = tokenManager.withAuthRetry(serverUri, token) { auth ->
                            service.listFiles(path, "Bearer $auth")
                        }
                        if (response.isSuccessful && response.body() != null) {
                            newAvail[path] = true
                            newCounts[path] = response.body()?.size ?: 0
                        } else {
                            newAvail[path] = false
                        }
                    } catch (_: Exception) {
                        newAvail[path] = false
                    }
                }
                dirAvailability = newAvail
                dirItemCounts = newCounts
            }
        } else if (isServerOnline == false) {
            dirAvailability = markedDirs.keys.associateWith { false }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "MARKED DIRECTORIES",
            actionText = if (markedDirs.isNotEmpty()) "${markedDirs.size} Marked" else "Add in Drive",
            accentColor = accentColor,
            onActionClick = { onNavigateToDrive(null) }
        )

        if (markedDirs.isEmpty()) {
            InsetGroupedGlassCard(
                modifier = Modifier.clickable { onNavigateToDrive(null) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, GlassHairline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No Marked Directories",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Open My Drive, tap any folder's options, and select 'Mark Folder as' to pin your Movie or Series collections here.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else if (markedDirs.size == 1) {
            val (path, type) = markedDirs.entries.first()
            MarkedDirectoryTile(
                path = path,
                mediaType = type,
                isAvailable = dirAvailability[path],
                itemCount = dirItemCounts[path],
                onClick = { onNavigateToDrive(path) },
                onRemoveClick = {
                    mediaDirManager.setDirectoryType(path, DirectoryMediaType.NONE)
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            markedDirs.toList().chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (path, type) ->
                        MarkedDirectoryTile(
                            path = path,
                            mediaType = type,
                            isAvailable = dirAvailability[path],
                            itemCount = dirItemCounts[path],
                            onClick = { onNavigateToDrive(path) },
                            onRemoveClick = {
                                mediaDirManager.setDirectoryType(path, DirectoryMediaType.NONE)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
