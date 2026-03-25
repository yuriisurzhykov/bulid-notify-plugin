package me.yuriisoft.buildnotify.mobile.data.discovery

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

/**
 * Abstracts mDNS / NSD service discovery.
 *
 * Concrete platform implementations:
 *   Android — [AndroidNsdDiscovery] via NsdManager
 *   iOS     — [IosNsdDiscovery] via NSNetServiceBrowser (cinterop)
 *
 * The domain layer depends only on [INsdRepository][me.yuriisoft.buildnotify.mobile.domain.repository.INsdRepository];
 * this interface sits one level below, inside the data layer, as the platform boundary (DIP).
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
