package me.yuriisoft.buildnotify.mobile.network.error.recognizers

import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer

/**
 * Recognizes connection-refused and host-unreachable errors.
 *
 * Platform-specific exception types (`java.net.ConnectException` on JVM,
 * `NSPOSIXErrorDomain` on Darwin) are not available in common code, so
 * this recognizer walks the cause chain looking for known refusal signals
 * in the exception messages — a pragmatic KMP trade-off.
 */
class RefusedErrors : ErrorRecognizer {

    override fun recognize(throwable: Throwable): ConnectionErrorReason? = when {
        throwable is java.net.UnknownServiceException -> ConnectionErrorReason.Refused(
            throwable.message ?: "Unknown service requested"
        )

        throwable.isConnectionRefused()               -> {
            ConnectionErrorReason.Refused(throwable.message ?: "Connection refused")
        }

        else                                          -> null
    }

    private fun Throwable.isConnectionRefused(): Boolean =
        generateSequence(this) { it.cause }
            .any { it.message?.containsAny(REFUSAL_PATTERNS) == true }

    private fun String.containsAny(keywords: List<String>): Boolean =
        keywords.any { contains(it, ignoreCase = true) }

    private companion object {
        val REFUSAL_PATTERNS = listOf("refused", "unreachable", "no route to host")
    }
}
