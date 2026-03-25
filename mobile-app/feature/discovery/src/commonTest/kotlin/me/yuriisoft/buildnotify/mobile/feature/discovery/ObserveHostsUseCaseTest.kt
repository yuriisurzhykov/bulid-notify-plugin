package me.yuriisoft.buildnotify.mobile.feature.discovery

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.yuriisoft.buildnotify.mobile.core.usecase.NoParams
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.testing.FakeNsdRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveHostsUseCaseTest {

    private val repository = FakeNsdRepository()
    private val useCase = ObserveHostsUseCase(repository)

    @Test
    fun emitsEmptyListWhenNoHostsDiscovered() = runTest {
        val hosts = useCase(NoParams).first()

        assertTrue(hosts.isEmpty())
    }

    @Test
    fun emitsHostsFromRepository() = runTest {
        val expected = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
        )
        repository.emit(expected)

        val actual = useCase(NoParams).first()

        assertEquals(expected, actual)
    }

    @Test
    fun reflectsRepositoryUpdates() = runTest {
        val first = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
        )
        val second = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
            DiscoveredHost(name = "Desktop", host = "192.168.1.10", port = 8766),
        )

        repository.emit(first)
        repository.emit(second)

        val snapshots = useCase(NoParams).take(1).toList()

        assertEquals(second, snapshots.last())
    }

    @Test
    fun delegatesToRepositoryDiscoverHosts() = runTest {
        val host = DiscoveredHost(name = "Linux", host = "10.0.0.1", port = 9000)
        repository.emit(listOf(host))

        val result = useCase(NoParams).first()

        assertEquals(1, result.size)
        assertEquals("Linux", result.first().name)
        assertEquals("10.0.0.1", result.first().host)
        assertEquals(9000, result.first().port)
    }
}
