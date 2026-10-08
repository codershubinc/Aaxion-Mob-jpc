package com.codershubinc.aaxion_mob.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun IpSelectorModal(
    currentActiveUri: String,
    prioritizedUri: String?,
    discoveredUris: List<String>,
    onSelectAndPrioritize: (String) -> Unit,
    onClearPriority: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var customIpInput by remember { mutableStateOf("") }
    var customPortInput by remember { mutableStateOf("8080") }

    // Combine discovered IPv4 IPs and current IP, keeping unique and only IPv4
    val allServers = remember(discoveredUris, currentActiveUri, prioritizedUri) {
        val list = mutableListOf<String>()
        // Helper to add only IPv4 hosts
        fun addIfIPv4(uri: String) {
            try {
                val u = android.net.Uri.parse(uri)
                val host = u.host ?: uri
                if (host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    list.add(uri)
                }
            } catch (e: Exception) {
                // ignore non-parseable URIs
            }
        }
        if (!prioritizedUri.isNullOrBlank()) {
            addIfIPv4(prioritizedUri)
        }
        if (currentActiveUri.isNotBlank()) {
            addIfIPv4(currentActiveUri)
        }
        for (disc in discoveredUris) {
            addIfIPv4(disc)
        }
        list
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AaxionSurface)
                .border(1.dp, AaxionBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AaxionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = AaxionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Server IP & Priority",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select or pin connection target",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prioritized IP Indicator or Reset
                if (!prioritizedUri.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AaxionPrimary.copy(alpha = 0.10f))
                            .border(1.dp, AaxionPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AaxionPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pinned: $prioritizedUri",
                                color = AaxionPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Auto/Reset",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClearPriority() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Available & Discovered Servers List
                Text(
                    text = "AVAILABLE TARGETS",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (allServers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AaxionSurfaceHigh)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Scanning network for Aaxion servers...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allServers) { uri ->
                            val isActive = uri == currentActiveUri
                            val isPrioritized = uri == prioritizedUri

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isActive) AaxionPrimary.copy(alpha = 0.12f) else AaxionSurfaceHigh)
                                    .border(
                                        1.dp,
                                        if (isActive) AaxionPrimary.copy(alpha = 0.4f) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onSelectAndPrioritize(uri)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                            fontSize = 13.sp,
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPrioritized) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Pinned",
                                            tint = AaxionPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (isPrioritized) "Pinned" else if (isActive) "Active" else "Prioritize",
                                        color = if (isActive) AaxionPrimary else TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom IP Input Section
                Text(
                    text = "MANUAL IP OVERRIDE",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customIpInput,
                        onValueChange = { customIpInput = it.trim() },
                        placeholder = { Text("192.168.1.xxx", color = TextTertiary, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AaxionPrimary,
                            unfocusedBorderColor = AaxionBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = AaxionSurfaceHigh,
                            unfocusedContainerColor = AaxionSurfaceHigh
                        ),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = customPortInput,
                        onValueChange = { customPortInput = it.trim() },
                        placeholder = { Text("Port", color = TextTertiary, fontSize = 12.sp) },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AaxionPrimary,
                            unfocusedBorderColor = AaxionBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = AaxionSurfaceHigh,
                            unfocusedContainerColor = AaxionSurfaceHigh
                        ),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (customIpInput.isNotBlank()) {
                                    val port = customPortInput.toIntOrNull() ?: 8080
                                    val formatted = if (customIpInput.startsWith("http://") || customIpInput.startsWith("https://")) {
                                        customIpInput
                                    } else {
                                        "http://$customIpInput:$port"
                                    }
                                    onSelectAndPrioritize(formatted)
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (customIpInput.isNotBlank()) {
                            val port = customPortInput.toIntOrNull() ?: 8080
                            val formatted = if (customIpInput.startsWith("http://") || customIpInput.startsWith("https://")) {
                                customIpInput
                            } else {
                                "http://$customIpInput:$port"
                            }
                            onSelectAndPrioritize(formatted)
                        }
                    },
                    enabled = customIpInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AaxionPrimary,
                        disabledContainerColor = AaxionPrimary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Prioritize & Connect",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
