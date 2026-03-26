package me.yuriisoft.buildnotify.mobile.network.transport

import kotlinx.serialization.json.Json
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope

/**
 * Thin codec that hides JSON serialization from the transport layer.
 *
 * [WebSocketTransport] depends on this object instead of on [Json] directly,
 * so a wire-format change (e.g. to Protobuf) only touches this class.
 */
class PayloadCodec(private val json: Json) {

    fun decode(text: String): WsEnvelope =
        json.decodeFromString(WsEnvelope.serializer(), text)

    fun encode(envelope: WsEnvelope): String =
        json.encodeToString(WsEnvelope.serializer(), envelope)
}
