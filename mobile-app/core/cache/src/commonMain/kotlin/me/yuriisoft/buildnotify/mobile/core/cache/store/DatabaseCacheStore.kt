package me.yuriisoft.buildnotify.mobile.core.cache.store

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import me.yuriisoft.buildnotify.mobile.core.cache.db.CacheEntryQueries

class DatabaseCacheStore<K : Any, V : Any>(
    private val queries: CacheEntryQueries,
    private val storeName: String,
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
    private val json: Json,
) : CacheStore<K, V> {

    override fun observeAll(): Flow<List<V>> =
        queries.selectAll(storeName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { json.decodeFromString(valueSerializer, it) } }

    override fun observe(key: K): Flow<V?> =
        queries.selectByKey(storeName, json.encodeToString(keySerializer, key))
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let { json.decodeFromString(valueSerializer, it) } }

    override suspend fun put(key: K, value: V) {
        queries.upsert(
            store_name = storeName,
            key = json.encodeToString(keySerializer, key),
            value_ = json.encodeToString(valueSerializer, value),
        )
    }

    override suspend fun get(key: K): V? =
        queries.selectByKey(storeName, json.encodeToString(keySerializer, key))
            .executeAsOneOrNull()
            ?.let { json.decodeFromString(valueSerializer, it) }

    override suspend fun remove(key: K) {
        queries.deleteByKey(storeName, json.encodeToString(keySerializer, key))
    }

    override suspend fun clear() {
        queries.deleteAll(storeName)
    }
}
