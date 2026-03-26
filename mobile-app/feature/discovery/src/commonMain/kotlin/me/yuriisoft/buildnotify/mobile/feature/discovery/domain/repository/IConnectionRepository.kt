package me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository

import kotlinx.coroutines.flow.StateFlow
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionStatus
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost

/**
 * Application-scoped contract for managing the WebSocket connection
 * to a Build Notify IDE plugin instance.
 *
 * Follows DIP: the domain and feature layers depend only on this abstraction;
 * the concrete implementation ([ConnectionManager] backed by Ktor) lives in
 * the data layer.
 *
 * [status] is the single source of truth for the connection lifecycle.
 */
interface IConnectionRepository {
    val status: StateFlow<ConnectionStatus>
    suspend fun connect(host: DiscoveredHost)
    suspend fun disconnect()
}
