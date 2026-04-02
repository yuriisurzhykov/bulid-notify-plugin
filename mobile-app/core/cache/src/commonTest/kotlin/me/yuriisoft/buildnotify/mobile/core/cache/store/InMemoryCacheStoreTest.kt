package me.yuriisoft.buildnotify.mobile.core.cache.store

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryCacheStoreTest {

    private fun createStore(): InMemoryCacheStore<String, String> = InMemoryCacheStore()

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
    fun observeEmitsNullThenValueOnPut() = runTest {
        val store = createStore()
        val emissions = mutableListOf<String?>()

        val job = launch {
            store.observe("key").take(2).toList(emissions)
        }
        advanceUntilIdle()

        store.put("key", "hello")
        advanceUntilIdle()

        job.join()
        assertEquals(listOf(null, "hello"), emissions)
    }

    @Test
    fun observeAllEmitsUpdatedListOnPut() = runTest {
        val store = createStore()
        val emissions = mutableListOf<List<String>>()

        val job = launch {
            store.observeAll().take(3).toList(emissions)
        }
        advanceUntilIdle()

        store.put("a", "1")
        advanceUntilIdle()

        store.put("b", "2")
        advanceUntilIdle()

        job.join()

        assertEquals(3, emissions.size)
        assertEquals(emptyList(), emissions[0])
        assertEquals(listOf("1"), emissions[1])
        assertEquals(listOf("1", "2"), emissions[2])
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
        val emissions = mutableListOf<List<String>>()

        val job = launch {
            store.observeAll().take(3).toList(emissions)
        }
        advanceUntilIdle()

        store.put("a", "1")
        store.put("b", "2")
        advanceUntilIdle()

        store.clear()
        advanceUntilIdle()

        job.join()
        assertEquals(emptyList(), emissions.last())
    }

    @Test
    fun observeEmitsDistinctValuesOnly() = runTest {
        val store = createStore()
        val emissions = mutableListOf<String?>()

        val job = launch {
            store.observe("key").take(3).toList(emissions)
        }
        advanceUntilIdle()

        store.put("key", "same")
        advanceUntilIdle()

        store.put("other", "irrelevant")
        advanceUntilIdle()

        store.put("key", "different")
        advanceUntilIdle()

        job.join()
        assertEquals(listOf(null, "same", "different"), emissions)
    }
}
