package me.yuriisoft.buildnotify.mobile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor

/**
 * iOS implementation of [INetworkMonitor] using NWPathMonitor via cinterop.
 *
 * NWPathMonitor wiring is deferred to Phase 4.
 * Returns `true` by default so that discovery is not blocked on iOS stubs.
 */
class IosNetworkMonitor : INetworkMonitor {

    override val isNetworkAvailable: StateFlow<Boolean> =
        MutableStateFlow(true)
}
