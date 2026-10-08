package com.codershubinc.aaxion_mob.ui

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
import com.codershubinc.aaxion_mob.ui.components.AaxionCard
import com.codershubinc.aaxion_mob.ui.theme.*
import com.codershubinc.aaxion_mob.utils.DataStoreManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionManagerScreen(
    onBackClick: () -> Unit,
    discoveredUris: List<String>,
    currentActiveUri: String,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }
    
    val prioritizedServerUris by dataStoreManager.prioritizedServerUris.collectAsState(initial = emptyList())
    val savedTunnelUri by dataStoreManager.tunnelServerUri.collectAsState(initial = "")
    
    var showCustomIpDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AaxionBg)
    ) {
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
                        Column(modifier = Modifier.padding(16.dp)) {
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
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                        focusedBorderColor = AaxionPrimary,
                                        unfocusedBorderColor = AaxionBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { 
                                        isEditingTunnel = false
                                        tunnelUriInput = savedTunnelUri ?: ""
                                    }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { 
                                            scope.launch { 
                                                dataStoreManager.setTunnelServerUri(tunnelUriInput)
                                                isEditingTunnel = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AaxionPrimary)
                                    ) {
                                        Text("Save")
                                    }
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
                                    }
                                    IconButton(onClick = { isEditingTunnel = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Tunnel URL", tint = AaxionPrimary)
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
                            Icon(Icons.Default.Add, contentDescription = "Add IP", modifier = Modifier.size(14.dp), tint = AaxionPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Manual IP", fontSize = 12.sp, color = AaxionPrimary, fontWeight = FontWeight.Bold)
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
                            backgroundColor = if (isPrioritized) AaxionPrimary.copy(alpha = 0.05f) else AaxionCardBg,
                            borderColor = if (isPrioritized) AaxionPrimary.copy(alpha = 0.3f) else AaxionBorder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                }

                                if (isPrioritized) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Priority $priorityIndex",
                                            color = AaxionPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
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
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = AaxionPrimary)
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
            }
        }
    }

    if (showCustomIpDialog) {
        var manualIp by remember { mutableStateOf("") }
        var manualPort by remember { mutableStateOf("8080") }

        AlertDialog(
            onDismissRequest = { showCustomIpDialog = false },
            containerColor = AaxionSurfaceHigh,
            title = {
                Text("Add Manual IP", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("IP Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AaxionPrimary,
                            unfocusedBorderColor = AaxionBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualPort,
                        onValueChange = { manualPort = it },
                        label = { Text("Port") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AaxionPrimary,
                            unfocusedBorderColor = AaxionBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
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
                }) {
                    Text("ADD IP", color = AaxionPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomIpDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}
