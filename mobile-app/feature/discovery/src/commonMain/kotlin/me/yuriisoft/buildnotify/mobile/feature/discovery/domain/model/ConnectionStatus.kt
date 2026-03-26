package me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model

/**
 * Single source of truth for the current state of the WebSocket connection
 * to a Build Notify IDE plugin instance.
 *
 * Observed by the UI layer (ViewModel) and the foreground service to react
 * to lifecycle transitions.
 */
sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data class Connecting(val host: DiscoveredHost) : ConnectionStatus
    data class Connected(val host: DiscoveredHost) : ConnectionStatus
    data class Error(
        val host: DiscoveredHost,
        val reason: ConnectionErrorReason,
    ) : ConnectionStatus
}
