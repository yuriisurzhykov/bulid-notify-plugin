package me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model

/**
 * Classifies why a WebSocket connection failed or was lost.
 *
 * Each subtype carries context specific to the failure mode, enabling the UI
 * to display targeted troubleshooting hints without resorting to string parsing.
 */
sealed interface ConnectionErrorReason {
    data class Timeout(val durationMs: Long) : ConnectionErrorReason
    data class Refused(val message: String) : ConnectionErrorReason
    data class HandshakeFailed(val message: String) : ConnectionErrorReason
    data class Lost(val message: String) : ConnectionErrorReason
    data class Unknown(val message: String) : ConnectionErrorReason
}
