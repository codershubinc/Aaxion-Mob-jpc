import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

tunnel_ui = """
                // SECTION: Remote Connection (Tunnel)
                SettingsSectionHeader("REMOTE CONNECTION (TUNNEL)")
                AaxionCard(backgroundColor = AaxionCardBg, borderColor = AaxionBorder) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Configure a backup/remote tunnel URL (e.g., Cloudflare Tunnel) to access your server when outside the local network.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (isEditingTunnel) {
                            OutlinedTextField(
                                value = tunnelUriInput,
                                onValueChange = { tunnelUriInput = it },
                                label = { Text("Tunnel URL (http/https)") },
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
"""

content = content.replace('                // SECTION: Storage & Cache', tunnel_ui + '\n                // SECTION: Storage & Cache')

with open("app/src/main/java/com/codershubinc/aaxion_mob/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
