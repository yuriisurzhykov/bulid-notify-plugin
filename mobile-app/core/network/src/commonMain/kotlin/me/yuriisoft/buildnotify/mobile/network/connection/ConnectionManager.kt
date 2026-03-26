package me.yuriisoft.buildnotify.mobile.network.connection

import kotlinx.coroutines.flow.StateFlow

/**
 * Control-plane view of the WebSocket connection.
 *
 * Orchestrators (DiscoveryViewModel, BuildMonitorService) depend on this
 * interface to drive and observe the connection lifecycle. They never
 * touch message payloads directly (ISP).
 *
 * [state] is the single source of truth for the connection lifecycle,
 * driven by the Flow pipeline inside the implementation.
 */
interface ConnectionManager {
    val state: StateFlow<ConnectionState>
    suspend fun connect(host: DiscoveredHost)
    suspend fun disconnect()
}
