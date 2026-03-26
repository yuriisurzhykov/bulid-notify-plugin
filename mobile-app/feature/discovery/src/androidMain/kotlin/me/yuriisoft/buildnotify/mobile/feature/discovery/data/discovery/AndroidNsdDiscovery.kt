package me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.PROTOCOL_DNS_SD
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Android implementation of [INsdDiscovery] backed by [NsdManager].
 *
 * Key design decisions:
 *
 * - **Sequential resolve queue** — On API < 34 the NsdManager can only resolve
 *   one service at a time; concurrent calls silently fail. A [Channel] serialises
 *   resolution requests so every discovered service is eventually resolved.
 *
 * - **callbackFlow** — Bridges the callback-based [NsdManager.DiscoveryListener]
 *   into a cold [Flow]. Discovery starts when the downstream collector appears
 *   and stops automatically when it is cancelled (via [awaitClose]).
 *
 * - **ConcurrentHashMap** — Maintains the live host list across interleaved
 *   "found" and "lost" callbacks arriving on NsdManager's internal thread.
 */
class AndroidNsdDiscovery(
    private val context: Context,
) : INsdDiscovery {

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    @Volatile
    private var activeListener: NsdManager.DiscoveryListener? = null

    override fun discoverServices(serviceType: String): Flow<List<DiscoveredHost>> =
        callbackFlow {
            val hosts = ConcurrentHashMap<String, DiscoveredHost>()
            val resolveQueue = Channel<NsdServiceInfo>(Channel.UNLIMITED)

            val resolverJob = launch {
                for (serviceInfo in resolveQueue) {
                    resolveService(serviceInfo)?.let { host ->
                        hosts[host.name] = host
                        trySend(hosts.values.toList())
                    }
                }
            }

            val listener = object : NsdManager.DiscoveryListener {
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    resolveQueue.trySend(serviceInfo)
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    hosts.remove(serviceInfo.serviceName)
                    trySend(hosts.values.toList())
                }

                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    close(NsdDiscoveryException(errorCode))
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            }

            activeListener = listener
            nsdManager.discoverServices(serviceType, PROTOCOL_DNS_SD, listener)

            awaitClose {
                resolverJob.cancel()
                activeListener = null
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (_: Exception) {
                }
            }
        }

    override fun stopDiscovery() {
        val listener = activeListener ?: return
        activeListener = null
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (_: Exception) {
        }
    }

    /**
     * Wraps the callback-based [NsdManager.resolveService] in a suspend function.
     *
     * Returns `null` when resolution fails (e.g. service disappeared before
     * resolution could complete) instead of throwing, because a single
     * unresolvable service should not cancel the entire discovery flow.
     */
    @Suppress("DEPRECATION")
    private suspend fun resolveService(serviceInfo: NsdServiceInfo): DiscoveredHost? =
        suspendCancellableCoroutine { cont ->
            nsdManager.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        if (!cont.isActive) return
                        val address = resolved.host?.hostAddress
                        if (address == null) {
                            cont.resume(null)
                            return
                        }
                        cont.resume(
                            DiscoveredHost(
                                name = resolved.serviceName,
                                host = address,
                                port = resolved.port,
                            ),
                        )
                    }
                },
            )
        }
}
