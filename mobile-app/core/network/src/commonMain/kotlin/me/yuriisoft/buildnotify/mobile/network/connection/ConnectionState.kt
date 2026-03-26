package me.yuriisoft.buildnotify.mobile.network.connection

import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason

/**
 * Finite state machine for the WebSocket connection lifecycle.
 *
 * Transitions are driven by the Flow pipeline inside `ManagedConnection`:
 *
 * ```
 * [*] → Idle
 * Idle → Connecting          (connect called)
 * Connecting → Connected     (first payload arrives)
 * Connected → Reconnecting   (error / heartbeat timeout)
 * Reconnecting → Connected   (payload after retry)
 * Reconnecting → Failed      (retries exhausted)
 * Connected → Disconnected   (disconnect called)
 * Connecting → Disconnected  (disconnect called)
 * Reconnecting → Disconnected(disconnect called)
 * Failed → Connecting        (connect called again)
 * Disconnected → Connecting  (connect called again)
 * ```
 *
 * Observed via [ConnectionManager.state] by the UI and foreground service.
 */
sealed interface ConnectionState {

    data object Idle : ConnectionState

    data class Connecting(val host: DiscoveredHost) : ConnectionState

    data class Connected(val host: DiscoveredHost) : ConnectionState

    data class Reconnecting(
        val host: DiscoveredHost,
        val attempt: Long,
    ) : ConnectionState

    data object Disconnected : ConnectionState

    data class Failed(
        val host: DiscoveredHost,
        val reason: ConnectionErrorReason,
    ) : ConnectionState
}
