package me.yuriisoft.buildnotify.mobile.network.error.recognizers

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.TimeoutCancellationException
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer

/**
 * Recognizes timeout-related exceptions from coroutines (`Flow.timeout()`),
 * Ktor's [HttpTimeout] plugin, and socket-level timeouts.
 */
class TimeoutErrors : ErrorRecognizer {

    override fun recognize(throwable: Throwable): ConnectionErrorReason? = when (throwable) {
        is TimeoutCancellationException -> {
            ConnectionErrorReason.Timeout(throwable.message ?: "Cancelled due to timeout")
        }

        is ConnectTimeoutException      -> {
            ConnectionErrorReason.Timeout(throwable.message ?: "Connect timed out")
        }

        is SocketTimeoutException       -> {
            ConnectionErrorReason.Timeout(throwable.message ?: "Socket timed out")
        }

        is HttpRequestTimeoutException  -> {
            ConnectionErrorReason.Timeout(throwable.message ?: "Request timed out")
        }

        else                            -> null
    }
}
