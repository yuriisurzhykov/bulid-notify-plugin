package me.yuriisoft.buildnotify.mobile.data.repository

import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.data.discovery.INsdDiscovery
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.domain.repository.INsdRepository

/**
 * Bridges the platform-specific [INsdDiscovery] with the domain-layer [INsdRepository].
 *
 * This thin adapter exists so that feature modules depend only on the domain interface
 * while the actual mDNS/NSD machinery remains an implementation detail of `:core:data`.
 */
@Inject
class NsdRepository(
    private val discovery: INsdDiscovery,
) : INsdRepository {

    override fun discoverHosts(serviceType: String): Flow<List<DiscoveredHost>> =
        discovery.discoverServices(serviceType)
}
