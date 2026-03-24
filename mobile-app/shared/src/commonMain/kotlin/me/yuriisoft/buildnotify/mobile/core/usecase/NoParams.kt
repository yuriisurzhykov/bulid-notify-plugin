package me.yuriisoft.buildnotify.mobile.core.usecase

/**
 * Singleton sentinel for use cases that need no input.
 *
 * ```kotlin
 * class GetCurrentUserUseCase : UseCase.Abstract<NoParams, User>() {
 *     override suspend fun execute(params: NoParams) = repository.currentUser()
 * }
 * val user = useCase(NoParams)
 * ```
 */
object NoParams