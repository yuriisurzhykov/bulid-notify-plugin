package me.yuriisoft.buildnotify.mobile.network.error

/**
 * Classifies why a WebSocket connection failed or was lost.
 *
 * Each subtype carries context specific to the failure mode, enabling the
 * UI to display targeted troubleshooting hints without resorting to string
 * parsing. New failure categories are added by creating a new subtype —
 * existing code remains untouched (OCP).
 *
 * Produced by [ErrorRecognizer] implementations and composed via the
 * `ErrorMapping` mapper (Phase 2).
 */
sealed interface ConnectionErrorReason {
    val message: String

    data class Timeout(override val message: String) : ConnectionErrorReason
    data class Refused(override val message: String) : ConnectionErrorReason
    data class HandshakeFailed(override val message: String) : ConnectionErrorReason
    data class Lost(override val message: String) : ConnectionErrorReason
    data class Unknown(override val message: String) : ConnectionErrorReason
}
