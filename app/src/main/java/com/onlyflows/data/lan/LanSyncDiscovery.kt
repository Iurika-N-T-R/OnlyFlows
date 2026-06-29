package com.onlyflows.data.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

private const val SERVICE_TYPE = "_onlyflows._tcp."

class LanSyncDiscovery(
    context: Context,
    private val port: Int,
    private val deviceName: String,
    private val onPeerFound: (ip: String, port: Int) -> Unit,
    private val onPeerLost: (name: String) -> Unit,
) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discovering = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(t: String, e: Int) { discovering = false }
        override fun onStopDiscoveryFailed(t: String, e: Int) {}
        override fun onDiscoveryStarted(t: String) { discovering = true }
        override fun onDiscoveryStopped(t: String) { discovering = false }
        override fun onServiceFound(info: NsdServiceInfo) {
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(i: NsdServiceInfo, e: Int) {}
                override fun onServiceResolved(i: NsdServiceInfo) {
                    val ip = i.host?.hostAddress ?: return
                    onPeerFound(ip, i.port)
                }
            })
        }
        override fun onServiceLost(info: NsdServiceInfo) = onPeerLost(info.serviceName)
    }

    fun start() {
        val info = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = SERVICE_TYPE
            this.port   = this@LanSyncDiscovery.port
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        })
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stop() {
        runCatching { if (discovering) nsd.stopServiceDiscovery(discoveryListener) }
    }
}
