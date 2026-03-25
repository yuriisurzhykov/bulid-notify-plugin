package me.yuriisoft.buildnotify.mobile.data.session

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.protocol.WsPayload

/**
 * Manages a single WebSocket connection to a Build Notify plugin instance.
 *
 * Responsibilities:
 *   - Opens / closes the Ktor [WebSocketSession]
 *   - Deserialises incoming [Frame.Text] into [WsEnvelope]
 *   - Serialises and sends outgoing [WsEnvelope]
 *   - Thread-safe send via [Mutex] (WebSocket frames must not interleave)
 *
 * This class is intentionally **not** a singleton — each connection attempt
 * creates a fresh session. The owning [BuildRepository] decides when to
 * reconnect.
 */
@Inject
class BuildSession(
    private val client: HttpClient,
    private val json: Json,
) {

    private var session: WebSocketSession? = null
    private val sendMutex = Mutex()

    /**
     * Opens a WebSocket to [host]:[port] and emits every deserialized [WsPayload].
     *
     * The flow completes when the server closes the connection.
     * Cancelling the collector closes the socket cleanly.
     */
    fun connect(host: String, port: Int): Flow<WsPayload> = flow {
        val ws = client.webSocketSession(host = host, port = port, path = "/ws")
        session = ws
        try {
            ws.incoming
                .receiveAsFlow()
                .mapNotNull { frame -> (frame as? Frame.Text)?.readText() }
                .collect { text ->
                    val envelope = json.decodeFromString<WsEnvelope>(text)
                    emit(envelope.payload)
                }
        } finally {
            close()
        }
    }

    /**
     * Sends [envelope] on the active session.
     * No-op when no session is open — avoids crashes during reconnection windows.
     */
    suspend fun send(envelope: WsEnvelope) {
        val ws = session ?: return
        sendMutex.withLock {
            val text = json.encodeToString(WsEnvelope.serializer(), envelope)
            ws.send(Frame.Text(text))
        }
    }

    private suspend fun close() {
        session?.close()
        session = null
    }
}
