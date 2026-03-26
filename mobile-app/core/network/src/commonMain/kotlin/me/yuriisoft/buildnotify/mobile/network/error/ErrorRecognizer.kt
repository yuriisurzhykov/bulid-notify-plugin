package me.yuriisoft.buildnotify.mobile.network.error

/**
 * A single link in the error-recognition chain.
 *
 * Each implementation examines a [Throwable] and returns a typed
 * [ConnectionErrorReason] if it can handle the exception, or `null`
 * to pass responsibility to the next recognizer.
 *
 * Adding a new recognizer for a new exception type requires no changes
 * to existing code — just a new class and a DI registration (OCP).
 */
fun interface ErrorRecognizer {
    fun recognize(throwable: Throwable): ConnectionErrorReason?
}
