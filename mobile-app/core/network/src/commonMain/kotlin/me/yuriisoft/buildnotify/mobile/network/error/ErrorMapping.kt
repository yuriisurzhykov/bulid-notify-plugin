package me.yuriisoft.buildnotify.mobile.network.error

import me.yuriisoft.buildnotify.mobile.core.mapper.Mapper

/**
 * Composes a chain of [ErrorRecognizer]s into a [Mapper] from [Throwable]
 * to [ConnectionErrorReason].
 *
 * Each recognizer is tried in order; the first non-null result wins.
 * If no recognizer matches, [ConnectionErrorReason.Unknown] is returned.
 *
 * To handle a new exception type, create a new [ErrorRecognizer] and add
 * it to the [recognizers] list in DI — no existing code is modified (OCP).
 */
class ErrorMapping(
    private val recognizers: List<ErrorRecognizer>,
) : Mapper<Throwable, ConnectionErrorReason> {

    override fun map(from: Throwable): ConnectionErrorReason =
        recognizers.firstNotNullOfOrNull { it.recognize(from) }
            ?: ConnectionErrorReason.Unknown(from.message ?: "Connection failed")
}
