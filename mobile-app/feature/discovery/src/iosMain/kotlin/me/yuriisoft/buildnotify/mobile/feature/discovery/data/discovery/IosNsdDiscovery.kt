package me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.darwin.NSObject

/**
 * iOS [INsdDiscovery] backed by [NSNetServiceBrowser] (Bonjour).
 *
 * Discovers Build Notify plugin instances advertised as `_buildnotify._tcp.`
 * on the local network. Each found service is resolved via
 * [NSNetService.resolveWithTimeout] to obtain its hostname and port.
 *
 * The browser is scheduled on the main run loop by default; the [callbackFlow]
 * bridge makes delegate callbacks safe to consume from any coroutine dispatcher.
 *
 * The resolved TXT record is now also queried for the `id` key — the stable
 * per-process UUID advertised by the plugin's [InstanceIdentity] service.
 * When present, it is stored in [DiscoveredHost.instanceId] and used by
 * [DiscoveryViewModel] as the [TrustedServers] lookup key.
 *
 * Cancelling the collecting coroutine stops the Bonjour browse session and
 * clears all delegate references to prevent retain cycles.
 */
class IosNsdDiscovery : INsdDiscovery {

    override fun discoverHosts(): Flow<List<DiscoveredHost>> = callbackFlow {
        val hosts = mutableMapOf<String, DiscoveredHost>()
        val resolving = mutableListOf<NSNetService>()

        fun sendSnapshot() {
            trySend(hosts.values.toList())
        }

        val browser = NSNetServiceBrowser()

        val browserDelegate = object : NSObject(), NSNetServiceBrowserDelegateProtocol {
            override fun netServiceBrowser(
                browser: NSNetServiceBrowser,
                didFindService: NSNetService,
                moreComing: Boolean,
            ) {
                val service = didFindService
                resolving += service

                val serviceDelegate = object : NSObject(), NSNetServiceDelegateProtocol {
                    override fun netServiceDidResolveAddress(sender: NSNetService) {
                        val hostName = sender.hostName ?: return
                        val txt = sender.parseTxtRecord()
                        hosts[sender.name] = DiscoveredHost(
                            name = sender.name,
                            host = hostName.trimEnd('.'),
                            port = sender.port.toInt(),
                            scheme = txt["scheme"] ?: "ws",
                            fingerprint = txt["fp"],
                            instanceId = txt["id"],
                        )
                        resolving.remove(sender)
                        sendSnapshot()
                    }

                    override fun netService(
                        sender: NSNetService,
                        didNotResolve: Map<Any?, *>,
                    ) {
                        resolving.remove(sender)
                    }
                }

                service.delegate = serviceDelegate
                service.resolveWithTimeout(RESOLVE_TIMEOUT)
            }

            override fun netServiceBrowser(
                browser: NSNetServiceBrowser,
                didRemoveService: NSNetService,
                moreComing: Boolean,
            ) {
                hosts.remove(didRemoveService.name)
                resolving.removeAll { it.name == didRemoveService.name }
                sendSnapshot()
            }
        }

        browser.delegate = browserDelegate
        browser.searchForServicesOfType(SERVICE_TYPE, inDomain = "")

        awaitClose {
            browser.stop()
            browser.delegate = null
            resolving.forEach { it.stop(); it.delegate = null }
            resolving.clear()
        }
    }

    private companion object {
        const val SERVICE_TYPE = "_buildnotify._tcp."
        const val RESOLVE_TIMEOUT = 5.0
    }
}

@Suppress("UNCHECKED_CAST")
private fun NSNetService.parseTxtRecord(): Map<String, String> {
    val data = TXTRecordData() ?: return emptyMap()
    val dict = NSNetService.dictionaryFromTXTRecordData(data) as? Map<String, NSData>
        ?: return emptyMap()
    return dict.mapNotNull { (key, value) ->
        val str = NSString.create(data = value, encoding = NSUTF8StringEncoding) as? String
        if (str.isNullOrEmpty()) null else key to str
    }.toMap()
}
