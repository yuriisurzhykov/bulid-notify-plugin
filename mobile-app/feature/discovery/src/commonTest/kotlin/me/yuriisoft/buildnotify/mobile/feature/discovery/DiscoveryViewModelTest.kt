package me.yuriisoft.buildnotify.mobile.feature.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.ObserveHostsUseCase
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionStatus
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryEvent
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryUiState
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryViewModel
import me.yuriisoft.buildnotify.mobile.testing.FakeNetworkMonitor
import me.yuriisoft.buildnotify.mobile.testing.RecordingEventCommunication
import me.yuriisoft.buildnotify.mobile.testing.RecordingStateCommunication
import me.yuriisoft.buildnotify.mobile.testing.TestAppDispatchers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {

    private val nsdRepository = FakeNsdRepository()
    private val connectionRepository = FakeConnectionRepository()
    private val networkMonitor = FakeNetworkMonitor(initiallyAvailable = true)
    private val dispatchers = TestAppDispatchers()
    private val useCase = ObserveHostsUseCase(nsdRepository)
    private val state = RecordingStateCommunication<DiscoveryUiState>(DiscoveryUiState.Scanning)
    private val events = RecordingEventCommunication<DiscoveryEvent>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatchers.main)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        scanTimeoutMs: Long = Long.MAX_VALUE,
        navigateDelayMs: Long = 0L,
    ) = DiscoveryViewModel(
        observeHosts = useCase,
        connectionRepository = connectionRepository,
        networkMonitor = networkMonitor,
        dispatchers = dispatchers,
        state = state,
        events = events,
        scanTimeoutMs = scanTimeoutMs,
        navigateDelayMs = navigateDelayMs,
    )

    // region Network gating

    @Test
    fun showsNetworkUnavailableWhenNoNetwork() {
        networkMonitor.setAvailable(false)

        createViewModel()

        assertIs<DiscoveryUiState.NetworkUnavailable>(state.observe.value)
    }

    @Test
    fun startsScanningWhenNetworkAvailable() {
        networkMonitor.setAvailable(true)

        createViewModel()

        assertIs<DiscoveryUiState.Scanning>(state.observe.value)
    }

    @Test
    fun recoversFromNetworkUnavailableWhenNetworkRestored() {
        networkMonitor.setAvailable(false)

        createViewModel()
        assertIs<DiscoveryUiState.NetworkUnavailable>(state.observe.value)

        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))
        networkMonitor.setAvailable(true)

        assertIs<DiscoveryUiState.ServiceSelection>(state.observe.value)
    }

    // endregion

    // region Discovery flow

    @Test
    fun showsNothingFoundAfterTimeout() {
        nsdRepository.emit(emptyList())

        createViewModel(scanTimeoutMs = 0L)

        assertIs<DiscoveryUiState.NothingFound>(state.observe.value)
    }

    @Test
    fun autoConnectsWhenSingleHostFound() {
        nsdRepository.emit(listOf(HOST_MACBOOK))

        createViewModel()

        assertEquals(1, connectionRepository.connectCalls.size)
        assertEquals(HOST_MACBOOK, connectionRepository.connectCalls.first())
        assertIs<DiscoveryUiState.Connected>(state.observe.value)
    }

    @Test
    fun showsServiceSelectionWhenMultipleHostsFound() {
        val hosts = listOf(HOST_MACBOOK, HOST_DESKTOP)
        nsdRepository.emit(hosts)

        createViewModel()

        val current = state.observe.value
        assertIs<DiscoveryUiState.ServiceSelection>(current)
        assertEquals(hosts, current.hosts)
    }

    @Test
    fun updatesServiceSelectionWhenHostsChange() {
        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))

        createViewModel()

        val updated = listOf(HOST_MACBOOK, HOST_DESKTOP, HOST_LINUX)
        nsdRepository.emit(updated)

        val current = state.observe.value
        assertIs<DiscoveryUiState.ServiceSelection>(current)
        assertEquals(updated, current.hosts)
    }

    @Test
    fun staysInScanningWhileNoHostsAndNoTimeout() {
        nsdRepository.emit(emptyList())

        createViewModel(scanTimeoutMs = Long.MAX_VALUE)

        assertIs<DiscoveryUiState.Scanning>(state.observe.value)
    }

    // endregion

    // region Connection flow

    @Test
    fun selectHostInitiatesConnection() {
        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))

        val vm = createViewModel()

        vm.selectHost(HOST_MACBOOK)

        assertEquals(1, connectionRepository.connectCalls.size)
        assertEquals(HOST_MACBOOK, connectionRepository.connectCalls.first())
    }

    @Test
    fun showsConnectedAfterSuccessfulConnection() {
        nsdRepository.emit(listOf(HOST_MACBOOK))

        createViewModel()

        assertIs<DiscoveryUiState.Connected>(state.observe.value)
    }

    @Test
    fun showsConnectionFailedOnError() {
        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))

        val vm = createViewModel()

        vm.selectHost(HOST_MACBOOK)

        connectionRepository.emitStatus(
            ConnectionStatus.Error(
                HOST_MACBOOK,
                ConnectionErrorReason.Refused("port closed"),
            ),
        )

        val current = state.observe.value
        assertIs<DiscoveryUiState.ConnectionFailed>(current)
        assertIs<ResText>(current.reason)
    }

    @Test
    fun emitsNavigateEventAfterConnected() {
        nsdRepository.emit(listOf(HOST_MACBOOK))

        createViewModel(navigateDelayMs = 0L)

        assertEquals(1, events.history.size)
        val event = events.history.first()
        assertIs<DiscoveryEvent.NavigateToBuild>(event)
        assertEquals(HOST_MACBOOK.host, event.host)
        assertEquals(HOST_MACBOOK.port, event.port)
    }

    // endregion

    // region Retry

    @Test
    fun retryRestartsScanFromServiceSelection() {
        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))

        val vm = createViewModel()
        assertIs<DiscoveryUiState.ServiceSelection>(state.observe.value)

        nsdRepository.emit(listOf(HOST_MACBOOK))
        vm.retry()

        assertIs<DiscoveryUiState.Connected>(state.observe.value)
    }

    @Test
    fun retryRestartsScanFromNothingFound() {
        nsdRepository.emit(emptyList())

        val vm = createViewModel(scanTimeoutMs = 0L)
        assertIs<DiscoveryUiState.NothingFound>(state.observe.value)

        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))
        vm.retry()

        assertIs<DiscoveryUiState.ServiceSelection>(state.observe.value)
    }

    // endregion

    // region Cancel

    @Test
    fun cancelStopsDiscoveryAndTransitionsToIdle() {
        nsdRepository.emit(emptyList())

        val vm = createViewModel()
        assertIs<DiscoveryUiState.Scanning>(state.observe.value)

        vm.cancel()

        assertIs<DiscoveryUiState.Idle>(state.observe.value)
    }

    @Test
    fun cancelStopsConnectionAndTransitionsToIdle() {
        nsdRepository.emit(listOf(HOST_MACBOOK, HOST_DESKTOP))

        val vm = createViewModel()
        assertIs<DiscoveryUiState.ServiceSelection>(state.observe.value)

        vm.selectHost(HOST_MACBOOK)
        assertIs<DiscoveryUiState.Connecting>(state.observe.value)

        vm.cancel()

        assertIs<DiscoveryUiState.Idle>(state.observe.value)
    }

    // endregion

    companion object {
        private val HOST_MACBOOK = DiscoveredHost(
            name = "MacBook",
            host = "192.168.1.5",
            port = 8765,
        )
        private val HOST_DESKTOP = DiscoveredHost(
            name = "Desktop",
            host = "192.168.1.10",
            port = 8766,
        )
        private val HOST_LINUX = DiscoveredHost(
            name = "Linux",
            host = "10.0.0.1",
            port = 9000,
        )
    }
}
