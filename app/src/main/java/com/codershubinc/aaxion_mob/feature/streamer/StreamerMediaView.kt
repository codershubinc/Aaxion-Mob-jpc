package com.codershubinc.aaxion_mob.feature.streamer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.core.network.buildStreamUrl
import com.codershubinc.aaxion_mob.feature.drive.FileViewModel

@Composable
fun StreamerMediaView(
    serverUri: String,
    token: String,
    viewModel: FileViewModel,
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val streamableMedia = viewModel.streamableMedia

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTablet && !isSidebarVisible) {
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AaxionCardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Show Sidebar",
                        tint = StreamerOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Text(
                    text = "Aaxion Streamer",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${streamableMedia.size} streamable media tracks in current directory",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
        }

        if (streamableMedia.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No video or audio tracks found in this directory",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Browse to a folder containing media files",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTablet) 220.dp else 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = streamableMedia,
                    key = { it.rawPath },
                    contentType = { if (it.isVideo) "video" else "audio" }
                ) { media ->
                    val isVideo = media.isVideo
                    val streamUrl = media.buildStreamUrl(serverUri, token)

                    AaxionCard(
                        backgroundColor = AaxionCardBg,
                        borderColor = if (isVideo) StreamerOrange.copy(alpha = 0.3f) else StreamerGreen.copy(alpha = 0.3f),
                        onClick = {
                            IntentUtils.launchStreamIntent(context, streamUrl, isVideo, title = media.name)
                        }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isTablet) 110.dp else 85.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isVideo) StreamerOrange.copy(alpha = 0.12f) else StreamerGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Default.PlayCircleOutline else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isVideo) StreamerOrange else StreamerGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = media.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isVideo) "VIDEO" else "AUDIO",
                                    color = if (isVideo) StreamerOrange else StreamerGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Stream",
                                    tint = if (isVideo) StreamerOrange else StreamerGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
