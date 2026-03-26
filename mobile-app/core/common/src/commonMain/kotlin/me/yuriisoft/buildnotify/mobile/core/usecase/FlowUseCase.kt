package me.yuriisoft.buildnotify.mobile.core.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Streaming use case: returns a cold [Flow] that emits values over time.
 *
 * Use this for long-lived observations (WebSocket events, NSD discovery,
 * database change listeners) instead of [UseCase] which is single-shot.
 *
 * Errors inside the flow are handled by the flow's own exception mechanism
 * (use `catch` operator at the call site, or `catch` inside [invoke]).
 *
 * ```kotlin
 * class ObserveBuildEventsUseCase @Inject constructor(
 *     private val repo: IBuildRepository,
 * ) : FlowUseCase.Abstract<ConnectionParams, BuildResult>() {
 *
 *     data class ConnectionParams(val host: String, val port: Int)
 *
 *     override fun execute(params: ConnectionParams): Flow<BuildResult> =
 *         repo.observeEvents(params.host, params.port)
 * }
 * ```
 */
interface FlowUseCase<in Params, out T> {

    operator fun invoke(params: Params): Flow<T>

    abstract class Abstract<in Params, out T> : FlowUseCase<Params, T> {

        protected abstract fun execute(params: Params): Flow<T>

        final override operator fun invoke(params: Params): Flow<T> = execute(params)
    }
}
