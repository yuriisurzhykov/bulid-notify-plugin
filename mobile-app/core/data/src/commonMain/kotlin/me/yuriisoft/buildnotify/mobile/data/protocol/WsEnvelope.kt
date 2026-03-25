package me.yuriisoft.buildnotify.mobile.data.protocol

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Every message on the wire, in both directions, is wrapped in this envelope.
 *
 * [version]             — protocol major version; client shows an upgrade prompt on mismatch.
 * [id]            — sender-generated UUID; used to deduplicate on reconnects.
 * [correlationId] — present only in cmd.result; links back to the originating command id.
 * [timestamp]            — epoch ms at creation time (not send time; may differ from server clock).
 * [payload]       — the typed message; the type discriminator "type" lives inside [payload].
 *
 * Mirror of the plugin's WsEnvelope — kept intentionally identical so the two ends
 * can evolve together without a separate shared artifact.
 */
@Serializable
data class WsEnvelope @OptIn(ExperimentalUuidApi::class) constructor(
    val version: Int = PROTOCOL_VERSION,
    val id: String = Uuid.random().toString(),
    val correlationId: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val payload: WsPayload,
) {
    companion object {
        /**
         * Bump only on breaking changes.
         * Additive changes (new optional fields, new payload subtypes) do NOT require a bump.
         */
        const val PROTOCOL_VERSION = 1
    }
}
