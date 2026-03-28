package me.yuriisoft.buildnotify.mobile.network.error.recognizers

import io.ktor.client.plugins.websocket.WebSocketException
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.HandshakeErrors.Companion.REJECTED_SIGNAL

/**
 * Recognizes WebSocket handshake / protocol failures and mTLS rejection errors.
 *
 * ### Recognition order (Chain of Responsibility)
 *
 * 1. **[ConnectionErrorReason.ClientRejected]** — checked first because it is a
 *    subcase of a TLS handshake failure. If the server's `ClientToFuTrustManager`
 *    explicitly rejected the client certificate, the JSSE stack on the server
 *    throws `CertificateException("Client explicitly rejected")`. OkHttp on Android
 *    and NSURLSession on iOS both surface this as an `SSLHandshakeException` (or a
 *    Darwin transport error) whose message contains [REJECTED_SIGNAL].
 *    Matching the exact signal string is a deliberate coupling to the server-side
 *    constant in `PersistentTrustedClients` — if that message ever changes, this
 *    recognizer must be updated in tandem (documented in both places).
 *
 * 2. **[ConnectionErrorReason.HandshakeFailed]** — generic TLS / WebSocket upgrade
 *    failure that does not match the rejection signal. Includes:
 *    - `CertificateException("Client approval pending")` — transient, retriable.
 *    - General WebSocket upgrade failures.
 *
 * ### Phase 5 change
 * Added the [REJECTED_SIGNAL] check that produces [ConnectionErrorReason.ClientRejected].
 * All existing behaviour for [WebSocketException] is unchanged.
 */
class HandshakeErrors : ErrorRecognizer {

    override fun recognize(throwable: Throwable): ConnectionErrorReason? {
        // Walk the full cause chain — the rejection message may be buried inside
        // an SSLHandshakeException wrapping a CertificateException.
        if (throwable.isClientRejected()) {
            return ConnectionErrorReason.ClientRejected(
                "This device was rejected by the IDE. Re-pair in IDE settings."
            )
        }

        return when (throwable) {
            is WebSocketException ->
                ConnectionErrorReason.HandshakeFailed(
                    throwable.message ?: "WebSocket handshake failed"
                )

            else -> null
        }
    }

    /**
     * Returns `true` if any exception in the cause chain contains [REJECTED_SIGNAL].
     *
     * Using `generateSequence` to walk the cause chain is safer than recursion
     * (avoids stack overflow on deeply nested causes) and cleaner than a loop.
     */
    private fun Throwable.isClientRejected(): Boolean =
        generateSequence(this) { it.cause }
            .any { it.message?.contains(REJECTED_SIGNAL, ignoreCase = false) == true }

    private companion object {

        /**
         * The exact exception message produced by the server's
         * `ClientToFuTrustManager` when the user explicitly rejects a device.
         *
         * **Contract:** this string MUST match the literal thrown by
         * `PersistentTrustedClients.reject()` path in `ClientToFuTrustManager`:
         * ```kotlin
         * throw CertificateException("Client explicitly rejected")
         * ```
         * Changing this value without updating the server-side message (or vice
         * versa) will cause the recognizer to silently fall through to
         * `HandshakeFailed`, losing the specific "rejected" UX treatment.
         */
        const val REJECTED_SIGNAL = "Client explicitly rejected"
    }
}