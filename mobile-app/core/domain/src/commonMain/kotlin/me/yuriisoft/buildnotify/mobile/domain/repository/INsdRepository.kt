package me.yuriisoft.buildnotify.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

/**
 * Discovers Build Notify plugin instances on the local network via mDNS/NSD.
 *
 * Follows DIP: the domain layer depends only on this abstraction;
 * platform-specific implementations (NsdManager on Android, NSNetServiceBrowser
 * on iOS) live in androidMain / iosMain respectively.
 */
interface INsdRepository {

    /**
     * Starts network service discovery and emits the current live list of
     * [DiscoveredHost]s every time it changes.
     *
     * The Flow completes when the collector is cancelled, which also stops discovery.
     */
    fun discoverHosts(serviceType: String = SERVICE_TYPE): Flow<List<DiscoveredHost>>

    companion object {
        /** mDNS service type registered by the Build Notify IDE plugin. */
        const val SERVICE_TYPE = "_buildnotify._tcp"
    }
}
