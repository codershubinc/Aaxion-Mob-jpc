package com.codershubinc.aaxion_mob.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class ServerDiscovery(context: Context, private val onServerFound: (ip: String, port: Int) -> Unit) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_aaxion._tcp"
    private var isDiscoveryStarted = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("ServerDiscovery", "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("ServerDiscovery", "Service discovery success: $service")
            // NsdManager might return serviceType with a dot at the end, or prefixed with a dot
            if (service.serviceType.contains(serviceType)) {
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e("ServerDiscovery", "Resolve failed: Error code: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d("ServerDiscovery", "Resolve Succeeded. $serviceInfo")
                        val port = serviceInfo.port
                        var host = serviceInfo.host.hostAddress
                        if (host != null) {
                            // Strip scope id if present (e.g. %wlan0)
                            if (host.contains("%")) {
                                host = host.substringBefore("%")
                            }
                            // Enclose IPv6 address in brackets for URL compatibility
                            if (host.contains(":")) {
                                host = "[$host]"
                            }
                            onServerFound(host, port)
                        }
                    }
                })
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e("ServerDiscovery", "Service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i("ServerDiscovery", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("ServerDiscovery", "Discovery failed: Error code: $errorCode")
            stopDiscovery()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("ServerDiscovery", "Stop discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun startDiscovery() {
        if (!isDiscoveryStarted) {
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                isDiscoveryStarted = true
            } catch (e: Exception) {
                Log.e("ServerDiscovery", "Failed to start discovery", e)
            }
        }
    }

    fun stopDiscovery() {
        if (isDiscoveryStarted) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e("ServerDiscovery", "Failed to stop discovery", e)
            }
            isDiscoveryStarted = false
        }
    }
}
