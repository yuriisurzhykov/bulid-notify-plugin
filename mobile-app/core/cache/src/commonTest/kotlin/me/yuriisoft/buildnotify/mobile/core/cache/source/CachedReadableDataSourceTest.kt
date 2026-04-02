package me.yuriisoft.buildnotify.mobile.core.cache.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CachedReadableDataSourceTest {

    private class FakeRemoteSource : ReadableDataSource<Unit, String> {
        private val _flow = MutableSharedFlow<String>()
        override fun observe(params: Unit): Flow<String> = _flow
        suspend fun emit(value: String) = _flow.emit(value)
    }

    private class FakeLocalSource : MutableDataSource<Unit, String> {
        val data = MutableStateFlow<String?>(null)
        override fun observe(params: Unit): Flow<String> = data.filterNotNull()
        override suspend fun save(params: Unit, data: String) { this.data.value = data }
        override suspend fun delete(params: Unit) { data.value = null }
    }

    private class TestCachedSource(
        remote: ReadableDataSource<Unit, String>,
        local: MutableDataSource<Unit, String>,
    ) : CachedReadableDataSource<Unit, String>(remote, local)

    @Test
    fun emitsFromLocalAfterRemoteDelivers() = runTest {
        val remote = FakeRemoteSource()
        val local = FakeLocalSource()
        val source = TestCachedSource(remote, local)

        val emissions = mutableListOf<String>()
        val job = launch {
            source.observe(Unit).collect { emissions.add(it) }
        }
        advanceUntilIdle()

        remote.emit("hello")
        advanceUntilIdle()

        assertEquals(listOf("hello"), emissions)
        job.cancel()
    }

    @Test
    fun localIsSourceOfTruth() = runTest {
        val remote = FakeRemoteSource()
        val local = FakeLocalSource()
        local.save(Unit, "local-value")

        val source = TestCachedSource(remote, local)
        val first = source.observe(Unit).first()
        assertEquals("local-value", first)
    }

    @Test
    fun remoteWritesToLocal() = runTest {
        val remote = FakeRemoteSource()
        val local = FakeLocalSource()
        val source = TestCachedSource(remote, local)

        val job = launch {
            source.observe(Unit).collect {}
        }
        advanceUntilIdle()

        remote.emit("remote-data")
        advanceUntilIdle()

        assertEquals("remote-data", local.data.value)
        job.cancel()
    }

    @Test
    fun continuesEmittingFromLocalWhenRemoteFails() = runTest {
        val local = FakeLocalSource()
        local.save(Unit, "cached")

        val failingRemote = object : ReadableDataSource<Unit, String> {
            override fun observe(params: Unit): Flow<String> =
                flow { throw RuntimeException("network error") }
        }

        val source = TestCachedSource(failingRemote, local)
        val result = source.observe(Unit).first()
        assertEquals("cached", result)
    }
}
