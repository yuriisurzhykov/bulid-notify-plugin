package me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost

/**
 * Platform abstraction for mDNS/NSD network service discovery.
 *
 * Platform-specific implementations ([AndroidNsdDiscovery], [IosNsdDiscovery])
 * are injected into [AppComponent] by the platform entry point. The discovery
 * feature module depends only on this interface (DIP).
 *
 * The service type (`_buildnotify._tcp.`) is an implementation detail of each
 * platform — callers never need to specify it.
 */
interface INsdDiscovery {

    /**
     * Starts mDNS discovery and emits the current live snapshot of
     * [DiscoveredHost]s every time a service appears, resolves, or disappears.
     *
     * The Flow never completes on its own — cancelling the collector stops
     * the underlying platform discovery session.
     */
    fun discoverHosts(): Flow<List<DiscoveredHost>>
}
