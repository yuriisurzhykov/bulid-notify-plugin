package me.yuriisoft.buildnotify.mobile.core.cache.store

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.yuriisoft.buildnotify.mobile.core.cache.db.CacheDatabase
import me.yuriisoft.buildnotify.mobile.core.cache.db.CacheEntryQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseCacheStoreTest {

    private fun createQueries(): CacheEntryQueries {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CacheDatabase.Schema.create(driver)
        return CacheDatabase(driver).cacheEntryQueries
    }

    private fun createStore(
        queries: CacheEntryQueries = createQueries(),
        storeName: String = "test_store",
    ): DatabaseCacheStore<String, String> =
        DatabaseCacheStore(
            queries = queries,
            storeName = storeName,
            keySerializer = String.serializer(),
            valueSerializer = String.serializer(),
            json = Json,
        )

    @Test
    fun putAndGetReturnsSameValue() = runTest {
        val store = createStore()
        store.put("key", "value")
        assertEquals("value", store.get("key"))
    }

    @Test
    fun getReturnsNullForMissingKey() = runTest {
        val store = createStore()
        assertNull(store.get("missing"))
    }

    @Test
    fun observeAllEmitsUpdatedListOnPut() = runTest {
        val store = createStore()

        val initial = store.observeAll().first()
        assertEquals(emptyList<String>(), initial)

        store.put("a", "value1")
        val afterFirst = store.observeAll().first()
        assertEquals(listOf("value1"), afterFirst)

        store.put("b", "value2")
        val afterSecond = store.observeAll().first()
        assertEquals(2, afterSecond.size)
        assertTrue(afterSecond.containsAll(listOf("value1", "value2")))
    }

    @Test
    fun removeDeletesEntry() = runTest {
        val store = createStore()
        store.put("key", "value")
        store.remove("key")
        assertNull(store.get("key"))
    }

    @Test
    fun clearRemovesAllEntries() = runTest {
        val store = createStore()
        store.put("a", "1")
        store.put("b", "2")
        store.clear()

        assertNull(store.get("a"))
        assertNull(store.get("b"))
        assertEquals(emptyList<String>(), store.observeAll().first())
    }

    @Test
    fun differentStoreNamesAreIsolated() = runTest {
        val queries = createQueries()
        val storeA = createStore(queries = queries, storeName = "store_a")
        val storeB = createStore(queries = queries, storeName = "store_b")

        storeA.put("key", "valueA")
        storeB.put("key", "valueB")

        assertEquals("valueA", storeA.get("key"))
        assertEquals("valueB", storeB.get("key"))

        storeA.clear()
        assertNull(storeA.get("key"))
        assertEquals("valueB", storeB.get("key"))
    }
}
