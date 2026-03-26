package me.yuriisoft.buildnotify.mobile.network.error.recognizers

import io.ktor.client.plugins.websocket.WebSocketException
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer

/**
 * Recognizes WebSocket handshake / protocol failures.
 *
 * Ktor throws [WebSocketException] (and its subclass `ProtocolViolationException`)
 * when the server rejects the upgrade or violates the WebSocket protocol.
 */
class HandshakeErrors : ErrorRecognizer {

    override fun recognize(throwable: Throwable): ConnectionErrorReason? = when (throwable) {
        is WebSocketException -> {
            ConnectionErrorReason.HandshakeFailed(throwable.message ?: "WebSocket handshake failed")
        }

        else                  -> null
    }
}
