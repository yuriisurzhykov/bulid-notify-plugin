package me.yuriisoft.buildnotify.mobile.core.usecase

import me.yuriisoft.buildnotify.mobile.core.either.Either
import me.yuriisoft.buildnotify.mobile.core.either.toEither

/**
 * Single-shot suspending use case: executes once and returns [Either].
 *
 * The [Abstract] base class wraps the concrete [execute] in `runCatching`,
 * so implementors never have to handle exceptions manually — any [Throwable]
 * thrown inside [execute] is automatically promoted to [Either.Left].
 *
 * ```kotlin
 * class FetchBuildHistoryUseCase @Inject constructor(
 *     private val repo: IBuildRepository,
 * ) : UseCase.Abstract<NoParams, List<BuildResult>>() {
 *
 *     override suspend fun execute(params: NoParams): List<BuildResult> =
 *         repo.getHistory()
 * }
 *
 * // In ViewModel:
 * viewModelScope.launch {
 *     fetchHistory(NoParams)
 *         .onSuccess { results -> _state.value = results }
 *         .onFailure { error   -> _error.value = error.message }
 * }
 * ```
 */
interface UseCase<in Params, out Result> {

    suspend fun invoke(params: Params): Either<Throwable, Result>

    abstract class Abstract<in Params, out Result> : UseCase<Params, Result> {

        /**
         * Implement the core business logic here.
         * Throw any exception freely — [invoke] wraps it in [Either.Left].
         */
        protected abstract suspend fun execute(params: Params): Result

        final override suspend fun invoke(
            params: Params,
        ): Either<Throwable, Result> = runCatching { execute(params) }.toEither()
    }
}
