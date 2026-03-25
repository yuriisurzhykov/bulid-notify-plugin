package me.yuriisoft.buildnotify.mobile.feature.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.testing.FakeNsdRepository
import me.yuriisoft.buildnotify.mobile.testing.TestAppDispatchers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {

    private val repository = FakeNsdRepository()
    private val dispatchers = TestAppDispatchers()
    private val useCase = ObserveHostsUseCase(repository)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatchers.main)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DiscoveryViewModel(useCase, dispatchers)

    @Test
    fun collectsFromRepositoryImmediatelyOnCreation() {
        val vm = createViewModel()

        assertIs<DiscoveryUiState.Content>(vm.state.value)
    }

    @Test
    fun showsHostsWhenDiscoverySucceeds() {
        val hosts = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
        )
        repository.emit(hosts)

        val vm = createViewModel()
        val state = vm.state.value

        assertIs<DiscoveryUiState.Content>(state)
        assertEquals(hosts, state.hosts)
    }

    @Test
    fun showsMultipleDiscoveredHosts() {
        val hosts = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
            DiscoveredHost(name = "Desktop", host = "192.168.1.10", port = 8766),
            DiscoveredHost(name = "Linux", host = "10.0.0.1", port = 9000),
        )
        repository.emit(hosts)

        val vm = createViewModel()
        val state = vm.state.value

        assertIs<DiscoveryUiState.Content>(state)
        assertEquals(3, state.hosts.size)
        assertEquals(hosts, state.hosts)
    }

    @Test
    fun updatesStateWhenHostsChange() {
        val initial = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
        )
        repository.emit(initial)

        val vm = createViewModel()

        val updated = listOf(
            DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765),
            DiscoveredHost(name = "Desktop", host = "192.168.1.10", port = 8766),
        )
        repository.emit(updated)

        val state = vm.state.value
        assertIs<DiscoveryUiState.Content>(state)
        assertEquals(updated, state.hosts)
    }

    @Test
    fun showsEmptyContentWhenNoHostsFound() {
        repository.emit(emptyList())

        val vm = createViewModel()
        val state = vm.state.value

        assertIs<DiscoveryUiState.Content>(state)
        assertEquals(emptyList(), state.hosts)
    }

    @Test
    fun `emits NavigateToBuild Event On Host Selection`() = runTest {
        val host = DiscoveredHost(name = "MacBook", host = "192.168.1.5", port = 8765)
        repository.emit(listOf(host))

        val vm = createViewModel()

        val events = mutableListOf<DiscoveryEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { events.add(it) }
        }

        vm.selectHost(host)

        assertEquals(1, events.size)
        val event = events.first()
        assertIs<DiscoveryEvent.NavigateToBuild>(event)
        assertEquals("192.168.1.5", event.host)
        assertEquals(8765, event.port)

        collectJob.cancel()
    }
}
