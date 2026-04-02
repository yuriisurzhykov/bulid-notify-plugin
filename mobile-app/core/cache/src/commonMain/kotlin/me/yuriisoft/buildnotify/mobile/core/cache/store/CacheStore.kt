package me.yuriisoft.buildnotify.mobile.core.cache.store

import kotlinx.coroutines.flow.Flow

interface CacheStore<K : Any, V : Any> {

    /**
     * Observes a single entry by [key]. Emits null if absent.
     * */
    fun observe(key: K): Flow<V?>

    /**
     * Observes all entries in this store.
     * */
    fun observeAll(): Flow<List<V>>

    /**
     * Returns the value for [key], or null if absent.
     * */
    suspend fun get(key: K): V?

    /**
     * Inserts or updates the entry for [key].
     * */
    suspend fun put(key: K, value: V)

    /**
     * Removes the entry for [key].
     * */
    suspend fun remove(key: K)

    /**
     * Removes all entries from this store.
     * */
    suspend fun clear()
}
