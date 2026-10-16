package com.codershubinc.aaxion_mob.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.codershubinc.aaxion_mob.ui.designsystem.AaxionCard
import com.codershubinc.aaxion_mob.ui.designsystem.*
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.core.data.DataStoreManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.core.network.ServerPingHelper
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionManagerScreen(
    onBackClick: () -> Unit,
    discoveredUris: List<String>,
    currentActiveUri: String,
    initialPrioritizedUris: List<String> = emptyList(),
    initialTunnelUri: String? = null,
    onRefresh: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    
    val prioritizedServerUris by dataStoreManager.prioritizedServerUris.collectAsState(initial = initialPrioritizedUris)
    val savedTunnelUri by dataStoreManager.tunnelServerUri.collectAsState(initial = initialTunnelUri ?: "")
    
    var showCustomIpDialog by remember { mutableStateOf(false) }

    val latencyMap = remember { mutableStateMapOf<String, Long?>() }

    // ONLY intercept system back when the dialog is open:
    BackHandler(enabled = showCustomIpDialog) {
        showCustomIpDialog = false
    }

    LaunchedEffect(prioritizedServerUris, discoveredUris, savedTunnelUri) {
        while (isActive) {
            val urisToPing = (prioritizedServerUris + discoveredUris + listOfNotNull(savedTunnelUri.takeIf { !it.isNullOrBlank() }))
                .filter { it.isNotBlank() }
                .distinct()
            coroutineScope {
                urisToPing.forEach { uri ->
                    launch {
                        val lat = ServerPingHelper.pingServer(uri = uri)
                        if (isActive) {
                            latencyMap[uri] = lat
                        }
                    }
                }
            }
            delay(3000.milliseconds)
        }
    }

    AmoledGlassBackground(accentColor = accentColor) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Connection Manager",
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
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EXPLANATION SECTION
                item {
                    Text(
                        text = "How Connection Priority Works",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                        Column {
                            Text(
                                text = "The app continuously pings servers to find the fastest active connection in this order:\n\n1. Priority List IPs (Ranked 0, 1, 2...)\n2. Auto-Discovered Local IPs (mDNS)\n3. Remote Tunnel URL (Fallback)",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // REMOTE TUNNEL SECTION
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "REMOTE CONNECTION (FALLBACK)",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    var tunnelUriInput by remember(savedTunnelUri) { mutableStateOf(savedTunnelUri ?: "") }
                    var isEditingTunnel by remember { mutableStateOf(false) }

                    AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                        Column {
                            Text(
                                text = "Used automatically when the local network is unreachable.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            if (isEditingTunnel) {
                                OutlinedTextField(
                                    value = tunnelUriInput,
                                    onValueChange = { tunnelUriInput = it },
                                    label = { Text("Tunnel URL (e.g. Cloudflare)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = AaxionBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { 
                                        isEditingTunnel = false
                                        tunnelUriInput = savedTunnelUri ?: ""
                                    }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    GlassButton(
                                        text = "Save",
                                        onClick = { 
                                            scope.launch { 
                                                dataStoreManager.setTunnelServerUri(tunnelUriInput)
                                                isEditingTunnel = false
                                            }
                                        },
                                        accentColor = accentColor
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (savedTunnelUri.isNullOrBlank()) "No Tunnel Configured" else savedTunnelUri!!,
                                            color = if (savedTunnelUri.isNullOrBlank()) TextMuted else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        if (!savedTunnelUri.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val lat = latencyMap[savedTunnelUri]
                                            val isOnline = lat != null
                                            val badgeColor = if (isOnline) Color(0xFF10B981) else TextMuted
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(badgeColor.copy(alpha = 0.12f))
                                                    .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(50))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isOnline) "$lat ms" else "Offline",
                                                    color = badgeColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { isEditingTunnel = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Tunnel URL", tint = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }

                // LOCAL NETWORK IPS SECTION
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL NETWORK IPs",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { showCustomIpDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add IP", modifier = Modifier.size(14.dp), tint = accentColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Manual IP", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val allIps = (prioritizedServerUris + discoveredUris).filter { it.isNotBlank() }.distinct()
                
                if (allIps.isEmpty()) {
                    item {
                        Text(
                            text = "No local IPs discovered yet.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(allIps) { uri ->
                        val priorityIndex = prioritizedServerUris.indexOf(uri)
                        val isPrioritized = priorityIndex != -1
                        val isActive = uri == currentActiveUri
                        
                        AaxionCard(
                            backgroundColor = if (isPrioritized) accentColor.copy(alpha = 0.08f) else AaxionCardBg,
                            borderColor = if (isPrioritized) accentColor.copy(alpha = 0.4f) else AaxionBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) Color(0xFF10B981) else TextTertiary)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = run {
                                                val u = android.net.Uri.parse(uri)
                                                val host = u.host ?: uri
                                                if (u.port != -1) "${host}:${u.port}" else host
                                            },
                                            color = if (isActive) TextPrimary else TextSecondary,
                                            fontSize = 15.sp,
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val lat = latencyMap[uri]
                                        val isOnline = lat != null
                                        val badgeColor = if (isOnline) Color(0xFF10B981) else TextMuted
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(badgeColor.copy(alpha = 0.12f))
                                                .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(50))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isOnline) "$lat ms" else "Offline",
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (isPrioritized) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(accentColor.copy(alpha = 0.14f))
                                                .border(0.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "Priority $priorityIndex",
                                                color = accentColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (priorityIndex > 0) {
                                            IconButton(
                                                onClick = {
                                                    val mutList = prioritizedServerUris.toMutableList()
                                                    val temp = mutList[priorityIndex - 1]
                                                    mutList[priorityIndex - 1] = uri
                                                    mutList[priorityIndex] = temp
                                                    scope.launch { dataStoreManager.setPrioritizedServerUris(mutList) }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = accentColor)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val mutList = prioritizedServerUris.toMutableList()
                                                mutList.remove(uri)
                                                scope.launch { dataStoreManager.setPrioritizedServerUris(mutList) }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Unpin", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Pin",
                                        color = TextTertiary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            val mutList = prioritizedServerUris.toMutableList()
                                            mutList.add(uri)
                                            scope.launch { dataStoreManager.setPrioritizedServerUris(mutList) }
                                        }.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showCustomIpDialog) {
        var manualIp by remember { mutableStateOf("") }
        var manualPort by remember { mutableStateOf("8080") }

        GlassDialog(
            onDismissRequest = { showCustomIpDialog = false },
            accentHighlight = accentColor
        ) {
            Text("Add Manual IP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("IP Address") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = AaxionBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = manualPort,
                onValueChange = { manualPort = it },
                label = { Text("Port") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = AaxionBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showCustomIpDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    text = "ADD IP",
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            val port = manualPort.toIntOrNull() ?: 8080
                            val formatted = if (manualIp.startsWith("http")) manualIp else "http://$manualIp:$port"
                            scope.launch {
                                val mutList = prioritizedServerUris.toMutableList()
                                if (!mutList.contains(formatted)) {
                                    mutList.add(formatted)
                                    dataStoreManager.setPrioritizedServerUris(mutList)
                                }
                            }
                        }
                        showCustomIpDialog = false
                    },
                    accentColor = accentColor
                )
            }
        }
    }
}
