package com.codershubinc.aaxion_mob.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codershubinc.aaxion_mob.models.FileItem
import com.codershubinc.aaxion_mob.ui.components.BackgroundGlows
import com.codershubinc.aaxion_mob.ui.theme.AaxionMobTheme
import androidx.compose.ui.tooling.preview.Preview
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    files: List<FileItem>,
    currentPath: String = "/",
    baseUrl: String,
    token: String,
    onFileClick: (FileItem) -> Unit,
    onBackClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
    ) {
        BackgroundGlows()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "File Explorer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(start = 8.dp, end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            if (isTablet) {
                // Grid layout for tablets
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(files) { file ->
                        FileGridCardItem(
                            file = file,
                            baseUrl = baseUrl,
                            token = token,
                            onClick = { onFileClick(file) }
                        )
                    }
                }
            } else {
                // List layout for phones
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(files) { file ->
                        FileListCardItem(
                            file = file,
                            baseUrl = baseUrl,
                            token = token,
                            onClick = { onFileClick(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileListCardItem(file: FileItem, baseUrl: String, token: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.01f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(if (!file.isDir && isImageFile(file.name)) 0.dp else 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!file.isDir && isImageFile(file.name)) {
                    val encodedPath = URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                    val thumbUrl = "$baseUrl/files/thumbnail?path=$encodedPath&tkn=$token"
                    
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (file.isDir) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (file.isDir) Color(0xFF3D5AFE) else Color(0xFF8B5CF6)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDir) {
                    Text(
                        text = formatFileSize(file.size),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = "Folder",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FileGridCardItem(file: FileItem, baseUrl: String, token: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.01f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(if (!file.isDir && isImageFile(file.name)) 0.dp else 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!file.isDir && isImageFile(file.name)) {
                    val encodedPath = URLEncoder.encode(file.rawPath, StandardCharsets.UTF_8.toString())
                    val thumbUrl = "$baseUrl/files/thumbnail?path=$encodedPath&tkn=$token"
                    
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (file.isDir) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = if (file.isDir) Color(0xFF3D5AFE) else Color(0xFF8B5CF6)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            if (!file.isDir) {
                Text(
                    text = formatFileSize(file.size),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else {
                Text(
                    text = "Folder",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

fun isImageFile(fileName: String): Boolean {
    val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return imageExtensions.contains(extension)
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Preview(showBackground = true)
@Composable
fun FileExplorerPreview() {
    val sampleFiles = listOf(
        FileItem("0e4200845ca28ec89a02a66c6624e300.jpg", false, 126983, "/home/swap/Downloads/wallpapers", "/home/swap/Downloads/wallpapers/0e4200845ca28ec89a02a66c6624e300.jpg"),
        FileItem("instagram_hd_1783705355617.jpg", false, 629077, "/home/swap/Downloads/wallpapers", "/home/swap/Downloads/wallpapers/instagram_hd_1783705355617.jpg"),
        FileItem("Documents", true, 0, "/home/swap", "/home/swap/Documents")
    )

    AaxionMobTheme {
        FileExplorerScreen(
            files = sampleFiles,
            currentPath = "/home/swap/Downloads/wallpapers",
            baseUrl = "http://localhost:8080",
            token = "sample_token",
            onFileClick = {},
            onBackClick = {}
        )
    }
}
