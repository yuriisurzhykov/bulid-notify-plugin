package me.yuriisoft.buildnotify.mobile.data.discovery

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

/**
 * iOS implementation of [INsdDiscovery] using NSNetServiceBrowser via cinterop.
 *
 * NSNetServiceBrowser wiring is deferred to Phase 4.
 * The kotlin-inject DI component defined in iosMain's entry point
 * will supply required platform dependencies at runtime.
 */
class IosNsdDiscovery : INsdDiscovery {

    override fun discoverServices(serviceType: String): Flow<List<DiscoveredHost>> =
        TODO("Phase 4 — NSNetServiceBrowser implementation")

    override fun stopDiscovery(): Unit =
        TODO("Phase 4 — NSNetServiceBrowser teardown")
}
