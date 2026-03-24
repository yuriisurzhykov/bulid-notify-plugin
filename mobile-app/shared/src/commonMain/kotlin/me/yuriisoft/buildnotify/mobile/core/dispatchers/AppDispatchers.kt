package me.yuriisoft.buildnotify.mobile.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Coroutine dispatcher abstraction for the entire KMP codebase.
 *
 * Motivations:
 *   1. Testability — swap [Default] for a test implementation without touching call sites.
 *   2. Platform safety — [Dispatchers.IO] on iOS maps to [Dispatchers.Default] automatically
 *      since coroutines 1.7+, so no expect/actual is needed.
 *   3. Explicit contract — callers express *intent* (UI / background) rather than
 *      picking a raw dispatcher, making accidental main-thread I/O impossible.
 *
 * Usage:
 * ```kotlin
 * class MyViewModel(private val d: AppDispatchers) {
 *     fun load() = d.launchBackground(viewModelScope) {
 *         val data = repository.fetch()
 *         d.withUi { _state.value = data }
 *     }
 * }
 * ```
 *
 * For unit tests, create a subclass of [Abstract] passing [UnconfinedTestDispatcher]
 * for all three dispatchers (lives in commonTest, not here).
 */
interface AppDispatchers {

    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher

    /** Launches [block] on the UI/main dispatcher. */
    fun launchUi(
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    /** Launches [block] on the IO dispatcher (background work, network, disk). */
    fun launchBackground(
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    /** Suspends, switches to the UI dispatcher, executes [block], then resumes. */
    suspend fun <T> withUi(block: suspend CoroutineScope.() -> T): T

    /** Suspends, switches to the IO dispatcher, executes [block], then resumes. */
    suspend fun <T> withBackground(block: suspend CoroutineScope.() -> T): T

    abstract class Abstract(
        override val main: CoroutineDispatcher,
        override val io: CoroutineDispatcher,
        override val default: CoroutineDispatcher,
    ) : AppDispatchers {

        final override fun launchUi(
            scope: CoroutineScope,
            block: suspend CoroutineScope.() -> Unit,
        ): Job = scope.launch(main, block = block)

        final override fun launchBackground(
            scope: CoroutineScope,
            block: suspend CoroutineScope.() -> Unit,
        ): Job = scope.launch(io, block = block)

        final override suspend fun <T> withUi(
            block: suspend CoroutineScope.() -> T,
        ): T = withContext(main, block = block)

        final override suspend fun <T> withBackground(
            block: suspend CoroutineScope.() -> T,
        ): T = withContext(io, block = block)
    }

    /**
     * Real dispatchers wired to the Kotlin coroutines defaults.
     * On iOS, [Dispatchers.IO] is automatically redirected to [Dispatchers.Default]
     * by the coroutines runtime — no platform-specific code required.
     *
     * Bind this in the kotlin-inject component:
     * ```kotlin
     * @Provides fun appDispatchers(): AppDispatchers = AppDispatchers.Default()
     * ```
     */
    class Default : Abstract(
        main = Dispatchers.Main,
        io = Dispatchers.IO,
        default = Dispatchers.Default,
    )
}
