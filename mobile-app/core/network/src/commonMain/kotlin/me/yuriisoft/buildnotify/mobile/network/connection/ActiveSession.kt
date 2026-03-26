package me.yuriisoft.buildnotify.mobile.network.connection

import kotlinx.coroutines.flow.SharedFlow
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.protocol.WsPayload

/**
 * Data-plane view of the WebSocket connection.
 *
 * Features that read or write messages depend on this interface — they
 * never see lifecycle concerns (ISP). A `BuildRepository`, for example,
 * subscribes to [incoming] filtered by payload type and sends commands
 * through [send].
 *
 * The [incoming] flow replays nothing (`replay = 0`); late subscribers
 * only see messages that arrive after they start collecting.
 */
interface ActiveSession {
    val incoming: SharedFlow<WsPayload>
    suspend fun send(envelope: WsEnvelope)
}
