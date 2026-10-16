package com.codershubinc.aaxion_mob.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.data.DataStoreManager
import com.codershubinc.aaxion_mob.core.model.AccentTheme
import com.codershubinc.aaxion_mob.core.model.IconPackTheme
import com.codershubinc.aaxion_mob.core.util.IntentUtils
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.designsystem.previewFolderIcon
import com.codershubinc.aaxion_mob.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, coil.annotation.ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(
    serverUri: String,
    token: String,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    wrapWithBackground: Boolean = false
) {
    val accentColor = LocalAccentColor.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val dataStoreManager = remember { DataStoreManager(context) }

    val savedAccentId by dataStoreManager.accentTheme.collectAsState(initial = null)
    val savedIconPackId by dataStoreManager.iconPackTheme.collectAsState(initial = null)
    val hideStatusBar by dataStoreManager.hideStatusBar.collectAsState(initial = false)
    val hideNavBar by dataStoreManager.hideNavigationBar.collectAsState(initial = false)

    val currentAccent = remember(savedAccentId) {
        AccentTheme.fromId(savedAccentId)
    }
    val currentIconPack = remember(savedIconPackId) {
        IconPackTheme.fromId(savedIconPackId)
    }


    val savedTunnelUri by dataStoreManager.tunnelServerUri.collectAsState(initial = "")
    var tunnelUriInput by remember(savedTunnelUri) { mutableStateOf(savedTunnelUri ?: "") }
    var isEditingTunnel by remember { mutableStateOf(false) }

    val isRefreshingToken by tokenManager.isRefreshing.collectAsState()
    val lastRefreshTs by tokenManager.lastRefreshTimestamp.collectAsState()
    val activeMasterToken by tokenManager.masterTokenState.collectAsState()
    val activeAccessToken by tokenManager.accessTokenState.collectAsState()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    val content = @Composable {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = if (wrapWithBackground) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AaxionCardBg)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION: Server & Connection
                SettingsSectionHeader("SERVER & CONNECTION")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    SettingsRow(
                        icon = Icons.Default.Dns,
                        iconTint = accentColor,
                        title = "Target Server Host",
                        subtitle = serverUri.ifBlank { "Auto-discovered Host" }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InsetGlassDivider(startIndent = 48.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsRow(
                        icon = Icons.Default.CheckCircle,
                        iconTint = AaxionSuccess,
                        title = "Connection State",
                        subtitle = "Connected & Active"
                    )
                }

                // SECTION: Security & Tokens
                SettingsSectionHeader("SECURITY & TOKEN ENGINE")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        iconTint = StreamerOrange,
                        title = "Master Token",
                        subtitle = if (!activeMasterToken.isNullOrBlank()) "••••••••••••${activeMasterToken!!.takeLast(6)}" else "Available"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Used automatically to regenerate access tokens every 4 hours.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    InsetGlassDivider(startIndent = 48.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRow(
                        icon = Icons.Default.Key,
                        iconTint = accentColor,
                        title = "Bearer Access Token",
                        subtitle = if (!activeAccessToken.isNullOrBlank()) "••••••••••••${activeAccessToken!!.takeLast(6)}" else "Active"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Attached to all Drive file operations, media streams, and image requests.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    InsetGlassDivider(startIndent = 48.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Last Token Refresh",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (lastRefreshTs > 0) dateFormat.format(Date(lastRefreshTs)) else "Upon sign-in",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        GlassButton(
                            text = "Refresh Now",
                            onClick = {
                                scope.launch {
                                    val success = tokenManager.refreshAccessToken(serverUri)
                                    val msg = if (success) "Access token refreshed (valid 4 hours)" else "Token refresh failed"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            isLoading = isRefreshingToken,
                            enabled = !isRefreshingToken,
                            cornerRadius = 8.dp,
                            accentColor = accentColor
                        )
                    }
                }

                // SECTION: Appearance & Display
                SettingsSectionHeader("APPEARANCE")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        iconTint = Color(currentAccent.colorHex),
                        title = "Theme",
                        subtitle = "AMOLED True Black (#000000)"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InsetGlassDivider(startIndent = 48.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsRow(
                        icon = Icons.Default.Fullscreen,
                        iconTint = Color(currentAccent.colorHex),
                        title = "Immersive Mode",
                        subtitle = "Status bar and navigation bar hidden"
                    )
                }

                // SECTION: Accent Color Customization
                SettingsSectionHeader("ACCENT COLOR")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    Text(
                        text = "Choose UI Accent Color",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Applies to buttons, highlights, badges, and brand elements across Drive",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AccentTheme.values().size) { idx ->
                            val accent = AccentTheme.values()[idx]
                            val isSelected = currentAccent == accent
                            val color = Color(accent.colorHex)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        scope.launch {
                                            dataStoreManager.setAccentTheme(accent.id)
                                            Toast.makeText(context, "Accent set to ${accent.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else AaxionBorder,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = accent.title.substringBefore(" "),
                                    color = if (isSelected) Color.White else TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // SECTION: Icon Pack Style
                SettingsSectionHeader("ICON PACK THEME")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    Text(
                        text = "Explorer Vector Pack",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Visual design style for folders, media, and document files",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconPackTheme.values().forEach { pack ->
                            val isSelected = currentIconPack == pack
                            val accentColor = Color(currentAccent.colorHex)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.12f) else AaxionSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) accentColor.copy(alpha = 0.6f) else AaxionBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            dataStoreManager.setIconPack(pack.id)
                                            Toast.makeText(context, "Icon pack set to ${pack.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = pack.previewFolderIcon(),
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pack.title,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = pack.description,
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: Sign Out

                // SECTION: Server Connections
                SettingsSectionHeader("SERVER CONNECTIONS")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder, onClick = onNavigateToConnectionManager) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connection Manager",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Manage pinned local IP and remote Tunnel URL",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Connection Manager",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // SECTION: Storage & Cache
                SettingsSectionHeader("DISPLAY SETTINGS")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hide Status Bar",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Hide notification bar by default",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = hideStatusBar,
                                onCheckedChange = { 
                                    scope.launch { dataStoreManager.setHideStatusBar(it) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(currentAccent.colorHex), checkedTrackColor = Color(currentAccent.colorHex).copy(alpha = 0.5f))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        InsetGlassDivider(startIndent = 48.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hide Navigation Bar",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Hide system navigation bar by default",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = hideNavBar,
                                onCheckedChange = { 
                                    scope.launch { dataStoreManager.setHideNavigationBar(it) }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(currentAccent.colorHex), checkedTrackColor = Color(currentAccent.colorHex).copy(alpha = 0.5f))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                SettingsSectionHeader("STORAGE & CACHE")
                val initialCacheSize = remember {
                    val diskSize = Coil.imageLoader(context).diskCache?.size
                    if (diskSize != null) IntentUtils.formatFileSize(diskSize) else null
                }
                var cacheSizeString by remember { mutableStateOf(initialCacheSize ?: "Calculating...") }
                LaunchedEffect(Unit) {
                    if (initialCacheSize == null) {
                        withContext(Dispatchers.IO) {
                            val cacheDir = context.cacheDir.resolve("image_cache")
                            val sizeBytes = if (cacheDir.exists()) {
                                cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                            } else 0L
                            cacheSizeString = IntentUtils.formatFileSize(sizeBytes)
                        }
                    }
                }
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            iconTint = StreamerOrange,
                            title = "Thumbnail & Image Cache",
                            subtitle = "$cacheSizeString used"
                        )
                        TextButton(
                            onClick = {
                                val imageLoader = Coil.imageLoader(context)
                                imageLoader.memoryCache?.clear()
                                imageLoader.diskCache?.clear()
                                scope.launch(Dispatchers.IO) {
                                    val cacheDir = context.cacheDir.resolve("image_cache")
                                    if (cacheDir.exists()) {
                                        cacheDir.deleteRecursively()
                                    }
                                    withContext(Dispatchers.Main) {
                                        cacheSizeString = "0 B"
                                        Toast.makeText(context, "Memory and disk cache cleared", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text("CLEAR", color = StreamerOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // SECTION: Sign Out
                SettingsSectionHeader("ACCOUNT")
                AaxionCard(
                    backgroundColor = AaxionDanger.copy(alpha = 0.06f),
                    borderColor = AaxionDanger.copy(alpha = 0.2f),
                    onClick = onLogout
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = AaxionDanger,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sign Out & Terminate Session",
                                color = AaxionDanger,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clears master token, access token, and cache",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (wrapWithBackground) {
        AmoledGlassBackground(accentColor = accentColor) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}
