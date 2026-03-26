package me.yuriisoft.buildnotify.mobile.network.reconnection

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Stateless exponential-backoff strategy.
 *
 * The [attempt] parameter comes from `retryWhen`, so this class holds no
 * mutable counter and needs no `reset()` — thread-safe by design.
 *
 * Backoff formula: `baseDelay * 2^attempt`, clamped to [maxDelay].
 * Returns `false` (stop retrying) once [attempt] reaches [maxAttempts].
 */
class ExponentialBackoff(
    private val maxAttempts: Int = 5,
    private val baseDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 30.seconds,
) : ReconnectionStrategy {

    override suspend fun shouldRetry(cause: Throwable, attempt: Long): Boolean {
        if (attempt >= maxAttempts) return false
        val shift = attempt.coerceAtMost(MAX_SHIFT).toInt()
        val backoff = (baseDelay * (1 shl shift).toDouble()).coerceAtMost(maxDelay)
        delay(backoff)
        return true
    }

    private companion object {
        const val MAX_SHIFT = 30L
    }
}
