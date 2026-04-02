package me.yuriisoft.buildnotify.mobile.core.cache.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryCacheStore<K : Any, V : Any> : CacheStore<K, V> {

    private val entries = MutableStateFlow<Map<K, V>>(emptyMap())

    override fun observe(key: K): Flow<V?> =
        entries.map { it[key] }.distinctUntilChanged()

    override fun observeAll(): Flow<List<V>> =
        entries.map { it.values.toList() }.distinctUntilChanged()

    override suspend fun get(key: K): V? = entries.value[key]

    override suspend fun put(key: K, value: V) {
        entries.update { it + (key to value) }
    }

    override suspend fun remove(key: K) {
        entries.update { it - key }
    }

    override suspend fun clear() {
        entries.value = emptyMap()
    }
}
