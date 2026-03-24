package me.yuriisoft.buildnotify.mobile.data.discovery

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

/**
 * Abstracts mDNS / NSD service discovery.
 *
 * Concrete platform implementations:
 *   Android — [NsdDiscovery] via NsdManager
 *   iOS     — [NsdDiscovery] via NSNetServiceBrowser (cinterop)
 *
 * The expect/actual mechanism ensures each platform registers its own class
 * while the domain layer depends only on this interface (DIP).
 */
interface INsdDiscovery {

    /**
     * Starts discovery for [serviceType] and emits the updated list of live hosts
     * every time a service is added, removed, or resolved.
     *
     * Discovery stops automatically when the collector is cancelled.
     */
    fun discoverServices(serviceType: String): Flow<List<DiscoveredHost>>

    /**
     * Stops any in-progress discovery immediately.
     * Safe to call even when discovery is not running.
     */
    fun stopDiscovery()
}
