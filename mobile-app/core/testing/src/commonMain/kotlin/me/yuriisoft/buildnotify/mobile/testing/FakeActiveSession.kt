package me.yuriisoft.buildnotify.mobile.testing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.protocol.WsPayload
import me.yuriisoft.buildnotify.mobile.network.connection.ActiveSession

/**
 * In-memory fake of [ActiveSession] for unit tests.
 *
 * Call [emit] to push payloads into [incoming].
 * All [send] calls are recorded in [sentEnvelopes] for assertion.
 */
class FakeActiveSession : ActiveSession {

    private val _incoming = MutableSharedFlow<WsPayload>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<WsPayload> = _incoming.asSharedFlow()

    val sentEnvelopes: MutableList<WsEnvelope> = mutableListOf()

    override suspend fun send(envelope: WsEnvelope) {
        sentEnvelopes += envelope
    }

    suspend fun emit(payload: WsPayload) {
        _incoming.emit(payload)
    }
}
