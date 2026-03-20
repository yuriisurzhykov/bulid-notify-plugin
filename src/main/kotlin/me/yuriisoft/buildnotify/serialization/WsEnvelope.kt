package me.yuriisoft.buildnotify.serialization

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Every single message on the wire, in both directions, is wrapped in this envelope.
 *
 * Fields:
 *  v             — protocol major version; client disconnects or downgrades UI on mismatch.
 *  id            — sender-generated UUID; used by receiver to deduplicate on reconnects.
 *  correlationId — present only in cmd.result, links back to the command id that triggered it.
 *  ts            — epoch ms at the moment of creation (not sending; may differ from server clock).
 *  payload       — the actual typed message; type discriminator "type" is inside this object.
 */
@Serializable
data class WsEnvelope @OptIn(ExperimentalUuidApi::class) constructor(
    val v: Int = PROTOCOL_VERSION,
    val id: String = Uuid.random().toString(),
    val correlationId: String? = null,
    val ts: Long = System.currentTimeMillis(),
    val payload: WsPayload,
) {
    companion object {
        /**
         * Bump when a breaking change is introduced.
         * Minor/additive changes (new optional fields, new payload types) do NOT require a bump.
         */
        const val PROTOCOL_VERSION = 1
    }
}
