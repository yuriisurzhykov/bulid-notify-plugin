package me.yuriisoft.buildnotify.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Single codec for the entire WS protocol.
 *
 * classDiscriminator = "type" — the payload type tag appears as { "type": "build.started", ... }
 * ignoreUnknownKeys   = true  — old clients survive new server fields (forward-compatibility).
 * encodeDefaults      = true  — optional fields with defaults are always present in the JSON
 *                               so clients don't need null-checks for known optional fields.
 */
object MessageSerializer {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(envelope: WsEnvelope): String = json.encodeToString(envelope)

    fun decode(raw: String): WsEnvelope = json.decodeFromString(raw)
}
