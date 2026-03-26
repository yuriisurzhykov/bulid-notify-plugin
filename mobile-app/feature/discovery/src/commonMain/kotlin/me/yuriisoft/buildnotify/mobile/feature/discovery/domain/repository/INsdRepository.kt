package me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost

/**
 * Discovers Build Notify plugin instances on the local network via mDNS/NSD.
 *
 * The domain layer depends only on this abstraction (DIP);
 * the platform-specific implementation lives in the data layer.
 */
interface INsdRepository {

    /**
     * Starts network service discovery and emits the current live list of
     * [DiscoveredHost]s every time it changes.
     *
     * The Flow completes when the collector is cancelled, which also stops discovery.
     */
    fun discoverHosts(): Flow<List<DiscoveredHost>>
}
