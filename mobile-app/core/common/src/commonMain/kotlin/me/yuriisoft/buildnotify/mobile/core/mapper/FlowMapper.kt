package me.yuriisoft.buildnotify.mobile.core.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


/**
 * Transforms a [Flow] of [T] into a [Flow] of [S].
 *
 * Keeps the reactive pipeline intact — no collection happens inside the mapper.
 *
 * ```kotlin
 * class DtoToModelFlowMapper(
 *     private val item: Mapper<Dto, Model>,
 * ) : FlowMapper<Dto, Model> {
 *     override fun map(from: Flow<Dto>): Flow<Model> = from.map(item::map)
 * }
 * ```
 */
interface FlowMapper<in T, out S> {

    fun map(from: Flow<T>): Flow<S>

    /**
     * Convenience base: delegates each emission to a synchronous [Mapper].
     * Override only when you need operators other than `map` (e.g. `flatMapLatest`).
     */
    abstract class Abstract<in T, out S>(
        private val itemMapper: Mapper<T, S>,
    ) : FlowMapper<T, S> {
        final override fun map(from: Flow<T>): Flow<S> = from.map(itemMapper::map)
    }
}
