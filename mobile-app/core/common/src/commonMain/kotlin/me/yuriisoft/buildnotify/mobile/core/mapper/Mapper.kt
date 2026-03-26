package me.yuriisoft.buildnotify.mobile.core.mapper

/**
 * Pure synchronous transformation: [T] → [S].
 *
 * Variance annotations enforce correctness at the call site:
 *   - `in T`  — a mapper that accepts [Animal] can map [Cat] (contravariance)
 *   - `out S` — a mapper that produces [Animal] can stand in for one producing [Cat]
 *
 * Nested helpers remove boilerplate for the most common specialisations:
 *   - [Unit]     — side-effect-only mapper (e.g. logging, analytics)
 *   - [Identity] — no-op passthrough (useful as a null-object / stub in tests)
 *   - [List]     — lifts a single-item mapper to work on a [kotlin.collections.List]
 */
interface Mapper<in T, out S> {

    fun map(from: T): S

    /** Mapper whose output is [Unit] — useful for side-effecting transforms. */
    fun interface Unit<in T> : Mapper<T, kotlin.Unit>

    /** Returns the input unchanged. Useful as a no-op stub or base for decoration. */
    abstract class Identity<T> : Mapper<T, T> {
        final override fun map(from: T): T = from
    }

    /**
     * Lifts a single-item [Mapper] to operate over [kotlin.collections.List].
     *
     * ```kotlin
     * val hostMapper: Mapper<DtoHost, DiscoveredHost> = HostMapper()
     * val listMapper = Mapper.List(hostMapper)
     * val hosts: List<DiscoveredHost> = listMapper.map(dtoList)
     * ```
     */
    class List<in T, out S>(
        private val itemMapper: Mapper<T, S>,
    ) : Mapper<kotlin.collections.List<T>, kotlin.collections.List<S>> {
        override fun map(from: kotlin.collections.List<T>): kotlin.collections.List<S> =
            from.map(itemMapper::map)
    }
}
