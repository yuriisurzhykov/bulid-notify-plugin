package me.yuriisoft.buildnotify.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope

/**
 * Manages the WebSocket connection to a Build Notify plugin instance.
 *
 * Follows DIP: the domain layer depends only on this abstraction;
 * the concrete Ktor implementation lives in data/.
 */
interface IBuildRepository {

    /**
     * Opens a persistent WebSocket connection and emits every incoming [WsEnvelope].
     * The Flow completes when the connection is closed.
     * Cancelling the collector disconnects cleanly.
     */
    fun observeEvents(host: String, port: Int): Flow<WsEnvelope>

    /** Sends an outgoing [WsEnvelope] on the active connection. No-op if not connected. */
    suspend fun send(envelope: WsEnvelope)
}
