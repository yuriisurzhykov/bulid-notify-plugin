package me.yuriisoft.buildnotify.mobile.network.error.recognizers

import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.io.EOFException
import kotlinx.io.IOException
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer

/**
 * Recognizes exceptions that indicate an established connection was lost.
 *
 * [ClosedReceiveChannelException] fires when the server closes the WebSocket.
 * [EOFException] fires when the stream ends unexpectedly.
 * A general [IOException] that wasn't caught by earlier recognizers in the
 * chain (timeouts, refused) is also treated as a lost connection.
 */
class LostConnectionErrors : ErrorRecognizer {

    override fun recognize(throwable: Throwable): ConnectionErrorReason? {
        println("LostConnectionErrors: ${throwable::class}, message: ${throwable.message}")
        return when (throwable) {
            is ClosedReceiveChannelException -> {
                ConnectionErrorReason.Lost(throwable.message ?: "Connection closed by server")
            }

            is EOFException                  -> {
                ConnectionErrorReason.Lost(throwable.message ?: "Connection ended unexpectedly")
            }

            is IOException                   -> {
                ConnectionErrorReason.Lost(throwable.message ?: "Connection lost")
            }

            else                             -> null
        }
    }
}
