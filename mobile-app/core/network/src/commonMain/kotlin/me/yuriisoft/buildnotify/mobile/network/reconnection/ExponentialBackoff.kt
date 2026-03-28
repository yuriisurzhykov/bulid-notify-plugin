package me.yuriisoft.buildnotify.mobile.network.reconnection

import kotlinx.coroutines.delay
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.error.ErrorMapping
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [ReconnectionStrategy] that implements exponential back-off with a
 * configurable maximum number of attempts.
 *
 * Used by [ManagedConnection.retryWhen] to decide whether to retry and how
 * long to wait between retries.
 *
 * ### Retry eligibility
 * Not all errors are worth retrying:
 * - [ConnectionErrorReason.ClientRejected] is a **permanent** server-side
 *   decision — retrying will always result in the same `CertificateException`.
 *   The only remedy is for the user to manually clear the rejection in the IDE
 *   plugin's Trusted Clients settings panel and re-pair.
 *
 * The [cause] arriving in [shouldRetry] is the raw [Throwable] from the Flow
 * pipeline. We map it through [errorMapping] to get a typed [ConnectionErrorReason]
 * before making the retry decision, keeping the backoff logic decoupled from
 * exception message parsing (SRP / DIP).
 *
 * ### Phase 5 change
 * Added [ConnectionErrorReason.ClientRejected] non-retry guard.
 * [errorMapping] is now injected to enable this classification without coupling
 * [ExponentialBackoff] to string matching.
 *
 * @param maxAttempts   maximum number of retry attempts before giving up.
 * @param baseDelay     delay before the first retry, in milliseconds.
 * @param maxDelay      upper bound for the delay, regardless of [factor].
 * @param errorMapping  used to classify a [Throwable] into a [ConnectionErrorReason].
 */
class ExponentialBackoff(
    private val errorMapping: ErrorMapping,
    private val maxAttempts: Int = 5,
    private val baseDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 30.seconds,
) : ReconnectionStrategy {

    /**
     * Returns `true` iff the connection should be retried.
     *
     * Called by `retryWhen` with:
     * @param cause   the exception that caused the current connection attempt to fail.
     * @param attempt 0-based attempt index (0 = first failure, 1 = second failure, …).
     */
    override suspend fun shouldRetry(cause: Throwable, attempt: Long): Boolean {
        // Permanent failures must not be retried regardless of attempt count.
        if (isPermanentFailure(cause)) return false

        if (attempt >= maxAttempts) return false
        val shift = attempt.coerceAtMost(MAX_SHIFT).toInt()
        val backoff = (baseDelay * (1 shl shift).toDouble()).coerceAtMost(maxDelay)
        delay(backoff)
        return true
    }

    /**
     * Returns `true` for error categories that will never resolve through retrying
     * alone and require explicit user action on the IDE side.
     *
     * Currently only [ConnectionErrorReason.ClientRejected] is permanent.
     * Adding new permanent categories here is the single-point change needed
     * to extend the policy (OCP — no changes to [ManagedConnection] or callers).
     */
    private fun isPermanentFailure(cause: Throwable): Boolean =
        when (errorMapping.map(cause)) {
            is ConnectionErrorReason.ClientRejected -> true
            else                                    -> false
        }

    private companion object {
        private const val MAX_SHIFT = 30L
    }
}
