with open("app/src/main/java/com/codershubinc/aaxion_mob/utils/ServerDiscovery.kt", "r") as f:
    content = f.read()

new_resolve = """                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d("ServerDiscovery", "Resolve Succeeded. $serviceInfo")
                        val port = serviceInfo.port
                        
                        // Try to get IPv4 address if available
                        Thread {
                            try {
                                val hostname = serviceInfo.host.hostName
                                val addresses = java.net.InetAddress.getAllByName(hostname)
                                var host = addresses.firstOrNull { it is java.net.Inet4Address }?.hostAddress 
                                    ?: serviceInfo.host.hostAddress

                                if (host != null) {
                                    if (host.contains("%")) {
                                        host = host.substringBefore("%")
                                    }
                                    if (host.contains(":")) {
                                        host = "[$host]"
                                    }
                                    
                                    // Switch back to main/caller thread is handled by the callback usage,
                                    // but if onServerFound requires UI thread, we should use Handler
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        onServerFound(host, port)
                                    }
                                }
                            } catch (e: Exception) {
                                // Fallback
                                var host = serviceInfo.host.hostAddress
                                if (host != null) {
                                    if (host.contains("%")) {
                                        host = host.substringBefore("%")
                                    }
                                    if (host.contains(":")) {
                                        host = "[$host]"
                                    }
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        onServerFound(host, port)
                                    }
                                }
                            }
                        }.start()
                    }"""

# Need to replace the old onServiceResolved
import re
pattern = re.compile(r"override fun onServiceResolved\(serviceInfo: NsdServiceInfo\) \{.*?onServerFound\(host, port\)\s*\}\s*\}", re.DOTALL)
content = pattern.sub(new_resolve, content)

with open("app/src/main/java/com/codershubinc/aaxion_mob/utils/ServerDiscovery.kt", "w") as f:
    f.write(content)

