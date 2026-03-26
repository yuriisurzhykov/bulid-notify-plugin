package me.yuriisoft.buildnotify.mobile.network.reconnection

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffTest {

    private val cause = RuntimeException("test error")

    @Test
    fun retriesWithinMaxAttempts() = runTest {
        val backoff = ExponentialBackoff(
            maxAttempts = 3,
            baseDelay = 1.milliseconds,
            maxDelay = 100.milliseconds,
        )

        assertTrue(backoff.shouldRetry(cause, attempt = 0))
        assertTrue(backoff.shouldRetry(cause, attempt = 1))
        assertTrue(backoff.shouldRetry(cause, attempt = 2))
    }

    @Test
    fun stopsRetryingAtMaxAttempts() = runTest {
        val backoff = ExponentialBackoff(
            maxAttempts = 3,
            baseDelay = 1.milliseconds,
            maxDelay = 100.milliseconds,
        )

        assertFalse(backoff.shouldRetry(cause, attempt = 3))
    }

    @Test
    fun stopsRetryingBeyondMaxAttempts() = runTest {
        val backoff = ExponentialBackoff(
            maxAttempts = 2,
            baseDelay = 1.milliseconds,
            maxDelay = 100.milliseconds,
        )

        assertFalse(backoff.shouldRetry(cause, attempt = 10))
    }

    @Test
    fun zeroMaxAttemptsNeverRetries() = runTest {
        val backoff = ExponentialBackoff(
            maxAttempts = 0,
            baseDelay = 1.milliseconds,
            maxDelay = 100.milliseconds,
        )

        assertFalse(backoff.shouldRetry(cause, attempt = 0))
    }

    @Test
    fun backoffDelayIsExponential() = runTest {
        val backoff = ExponentialBackoff(
            maxAttempts = 5,
            baseDelay = 100.milliseconds,
            maxDelay = 10.seconds,
        )

        val before0 = testScheduler.currentTime
        backoff.shouldRetry(cause, attempt = 0)
        val delay0 = testScheduler.currentTime - before0

        val before1 = testScheduler.currentTime
        backoff.shouldRetry(cause, attempt = 1)
        val delay1 = testScheduler.currentTime - before1

        val before2 = testScheduler.currentTime
        backoff.shouldRetry(cause, attempt = 2)
        val delay2 = testScheduler.currentTime - before2

        // base * 2^0 = 100ms, base * 2^1 = 200ms, base * 2^2 = 400ms
        assertTrue(delay0 == 100L, "attempt 0 delay: expected 100ms, got ${delay0}ms")
        assertTrue(delay1 == 200L, "attempt 1 delay: expected 200ms, got ${delay1}ms")
        assertTrue(delay2 == 400L, "attempt 2 delay: expected 400ms, got ${delay2}ms")
    }

    @Test
    fun backoffClampsToMaxDelay() = runTest {
        val backoff = ExponentialBackoff(
            maxAttempts = 10,
            baseDelay = 1.seconds,
            maxDelay = 5.seconds,
        )

        val before = testScheduler.currentTime
        backoff.shouldRetry(cause, attempt = 8) // 1s * 2^8 = 256s → clamped to 5s
        val delay = testScheduler.currentTime - before

        assertTrue(delay == 5_000L, "expected 5000ms (clamped), got ${delay}ms")
    }

    @Test
    fun defaultConfigurationRetries5Times() = runTest {
        val backoff = ExponentialBackoff()

        for (attempt in 0L..4L) {
            assertTrue(backoff.shouldRetry(cause, attempt), "should retry at attempt $attempt")
        }
        assertFalse(backoff.shouldRetry(cause, attempt = 5), "should stop at attempt 5")
    }
}
