package me.yuriisoft.buildnotify.mobile.network.reconnection

/**
 * Decides whether a failed connection should be retried.
 *
 * The [attempt] parameter comes from the `retryWhen` Flow operator,
 * so implementations are **stateless** — no mutable counter, no `reset()`
 * method, thread-safe by design.
 *
 * Returning `true` causes the upstream Flow (transport) to restart.
 * The implementation may `delay()` before returning to apply backoff.
 * Returning `false` lets the error propagate to `onCompletion`.
 */
interface ReconnectionStrategy {
    suspend fun shouldRetry(cause: Throwable, attempt: Long): Boolean
}
