package me.yuriisoft.buildnotify.mobile.core.mapper

/**
 * Suspending transformation: [T] → [S].
 *
 * Use when the mapping itself involves I/O, a database lookup, or any
 * other coroutine-based work.
 */
interface SuspendMapper<in T, out S> {

    suspend fun map(from: T): S

    abstract class Identity<T> : SuspendMapper<T, T> {
        final override suspend fun map(from: T): T = from
    }

    class List<in T, out S>(
        private val itemMapper: SuspendMapper<T, S>,
    ) : SuspendMapper<kotlin.collections.List<T>, kotlin.collections.List<S>> {
        override suspend fun map(
            from: kotlin.collections.List<T>,
        ): kotlin.collections.List<S> = from.map { itemMapper.map(it) }
    }
}
