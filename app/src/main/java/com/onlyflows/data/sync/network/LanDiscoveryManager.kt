package com.onlyflows.data.sync.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.onlyflows.domain.sync.PeerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "LanDiscovery"
private const val SERVICE_TYPE = "_onlyflows._tcp"

class LanDiscoveryManager(private val context: Context) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _peers = MutableStateFlow<List<PeerInfo>>(emptyList())
    val peers: StateFlow<List<PeerInfo>> = _peers.asStateFlow()

    private var thisDeviceId: String = ""
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var isResolving = false

    fun start(deviceId: String, deviceName: String, port: Int) {
        thisDeviceId = deviceId
        registerService(deviceName, port)
        startDiscovery()
    }

    fun stop() {
        try { registrationListener?.let { nsdManager.unregisterService(it) } } catch (_: Exception) {}
        try { discoveryListener?.let { nsdManager.stopServiceDiscovery(it) } } catch (_: Exception) {}
        registrationListener = null
        discoveryListener = null
        _peers.value = emptyList()
    }

    private fun registerService(deviceName: String, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "OnlyFlows-${deviceName.take(10)}"
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Registered as ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started")
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.contains(SERVICE_TYPE.removeSuffix("."))) {
                    resolveNext(serviceInfo)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _peers.value = _peers.value.filter { it.displayName != serviceInfo.serviceName }
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveNext(info: NsdServiceInfo) {
        synchronized(resolveQueue) {
            resolveQueue.add(info)
            if (!isResolving) processQueue()
        }
    }

    private fun processQueue() {
        val info = synchronized(resolveQueue) { resolveQueue.removeFirstOrNull() } ?: run {
            isResolving = false
            return
        }
        isResolving = true
        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                synchronized(resolveQueue) { processQueue() }
            }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port
                // Use service name as displayName; deviceId will be confirmed via /ping
                val peer = PeerInfo(
                    deviceId    = serviceInfo.serviceName,
                    displayName = serviceInfo.serviceName,
                    host        = host,
                    port        = port,
                )
                // Deduplicate — real deviceId resolved after /ping in SyncRepositoryImpl
                val existing = _peers.value
                if (existing.none { it.host == host && it.port == port }) {
                    _peers.value = existing + peer
                }
                synchronized(resolveQueue) { processQueue() }
            }
        })
    }

    fun updatePeer(old: PeerInfo, confirmed: PeerInfo) {
        _peers.value = _peers.value.map { if (it.host == old.host && it.port == old.port) confirmed else it }
    }

    fun removePeer(deviceId: String) {
        _peers.value = _peers.value.filter { it.deviceId != deviceId }
    }
}
