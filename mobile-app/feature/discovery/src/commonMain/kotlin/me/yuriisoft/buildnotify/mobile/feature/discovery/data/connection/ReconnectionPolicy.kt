package me.yuriisoft.buildnotify.mobile.feature.discovery.data.connection

import kotlinx.coroutines.delay

/**
 * Exponential-backoff policy for WebSocket reconnection attempts.
 *
 * Delays: [baseDelayMs], 2x, 4x, 8x, 16x … capped at [maxDelayMs].
 * After [maxAttempts] consecutive failures the policy gives up, leaving the
 * caller's status as-is so the UI can offer a manual retry.
 *
 * Call [reset] after a successful reconnection to restart the counter.
 */
class ReconnectionPolicy(
    private val maxAttempts: Int = 5,
    private val baseDelayMs: Long = 1_000,
    private val maxDelayMs: Long = 30_000,
) {

    private var attempt = 0

    fun reset() {
        attempt = 0
    }

    /**
     * Suspends for the next backoff interval and returns `true`,
     * or returns `false` immediately when all attempts are exhausted.
     */
    suspend fun awaitNextAttempt(): Boolean {
        if (attempt >= maxAttempts) return false
        val delayMs = (baseDelayMs shl attempt).coerceAtMost(maxDelayMs)
        attempt++
        delay(delayMs)
        return true
    }
}
