package me.yuriisoft.buildnotify.mobile.network.transport

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.protocol.WsPayload

/**
 * Raw bidirectional WebSocket channel — the only class that touches Ktor
 * WebSocket APIs directly (SRP: I/O only).
 *
 * Returns a cold [Flow] of decoded payloads. Takes a [ReceiveChannel] for
 * outgoing messages — the actor pattern replaces the old `sendMutex`.
 *
 * When the flow is cancelled or errors, `finally` cleans up both the sender
 * coroutine and the WebSocket session. When `retryWhen` (applied downstream)
 * restarts the flow, a fresh [WebSocketSession] is created and the sender
 * reconnects to the same [outgoing] channel.
 */
class WebSocketTransport(
    private val client: HttpClient,
    private val codec: PayloadCodec,
) : Transport {

    override fun open(
        host: String,
        port: Int,
        outgoing: ReceiveChannel<WsEnvelope>,
    ): Flow<WsPayload> = channelFlow {
        val ws = client.webSocketSession(host = host, port = port, path = "/ws")

        val sender = launch {
            for (envelope in outgoing) {
                ws.send(Frame.Text(codec.encode(envelope)))
            }
        }

        try {
            ws.incoming
                .receiveAsFlow()
                .mapNotNull { (it as? Frame.Text)?.readText() }
                .map { codec.decode(it).payload }
                .collect { send(it) }
        } finally {
            sender.cancel()
            ws.close()
        }
    }
}
