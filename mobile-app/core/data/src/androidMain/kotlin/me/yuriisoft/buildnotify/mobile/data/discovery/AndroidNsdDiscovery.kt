package me.yuriisoft.buildnotify.mobile.data.discovery

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

/**
 * Android implementation of [INsdDiscovery] using [android.net.nsd.NsdManager].
 *
 * Context and NsdManager are injected by the kotlin-inject DI component
 * defined in androidMain's application entry point (Phase 4).
 */
class AndroidNsdDiscovery : INsdDiscovery {

    override fun discoverServices(serviceType: String): Flow<List<DiscoveredHost>> =
        TODO("Phase 4 — NsdManager implementation")

    override fun stopDiscovery(): Unit =
        TODO("Phase 4 — NsdManager teardown")
}
