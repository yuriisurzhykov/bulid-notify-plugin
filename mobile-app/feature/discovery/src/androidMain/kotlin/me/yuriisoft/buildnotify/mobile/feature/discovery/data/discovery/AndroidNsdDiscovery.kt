package me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost

/**
 * Android [INsdDiscovery] backed by [NsdManager].
 *
 * Discovers Build Notify plugin instances advertised as `_buildnotify._tcp.`
 * on the local network. Each found service is resolved to obtain its IP and
 * port before being emitted.
 *
 * The resolved TXT attributes are now also queried for the `id` key, which
 * the plugin's [InstanceIdentity] service advertises as a stable per-process
 * UUID. When present, this value is stored in [DiscoveredHost.instanceId] and
 * used by [DiscoveryViewModel] as the [TrustedServers] lookup key instead of
 * the mutable service name.
 *
 * Cancelling the collecting coroutine stops the NSD discovery session.
 */
class AndroidNsdDiscovery(
    context: Context,
) : INsdDiscovery {

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    override fun discoverHosts(): Flow<List<DiscoveredHost>> = callbackFlow {
        val hosts = mutableMapOf<String, DiscoveredHost>()

        val listener = object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                channel.close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager?.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit

                        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val address = info.hostAddresses.firstOrNull()?.hostAddress ?: return
                            val attrs = info.attributes
                            hosts[info.serviceName] = DiscoveredHost(
                                name = info.serviceName,
                                host = address,
                                port = info.port,
                                scheme = attrs?.txtString("scheme") ?: "ws",
                                fingerprint = attrs?.txtString("fp"),
                                instanceId = attrs?.txtString("id"),
                            )
                            trySend(hosts.values.toList())
                        }
                    },
                )
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                hosts.remove(serviceInfo.serviceName)
                trySend(hosts.values.toList())
            }
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)

        awaitClose {
            runCatching { nsdManager?.stopServiceDiscovery(listener) }
        }
    }

    private companion object {
        const val SERVICE_TYPE = "_buildnotify._tcp."
    }
}

private fun Map<String, ByteArray>.txtString(key: String): String? =
    get(key)?.decodeToString()?.takeIf { it.isNotEmpty() }
