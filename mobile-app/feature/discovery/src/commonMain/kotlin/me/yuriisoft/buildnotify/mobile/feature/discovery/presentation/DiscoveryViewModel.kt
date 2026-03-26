package me.yuriisoft.buildnotify.mobile.feature.discovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.error_connection_failed
import build_notify_mobile.feature.discovery.generated.resources.error_handshake
import build_notify_mobile.feature.discovery.generated.resources.error_lost
import build_notify_mobile.feature.discovery.generated.resources.error_refused
import build_notify_mobile.feature.discovery.generated.resources.error_timeout
import build_notify_mobile.feature.discovery.generated.resources.error_unknown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.yuriisoft.buildnotify.mobile.core.communication.EventCommunication
import me.yuriisoft.buildnotify.mobile.core.communication.StateCommunication
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor
import me.yuriisoft.buildnotify.mobile.core.usecase.FlowUseCase
import me.yuriisoft.buildnotify.mobile.core.usecase.NoParams
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionState
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource

/**
 * Combines three data sources into a single [DiscoveryUiState]:
 *
 *  1. [INetworkMonitor]       — gates mDNS scan (WiFi/Ethernet only)
 *  2. [observeHosts] use case — emits discovered hosts via NSD
 *  3. [connectionManager]     — WebSocket lifecycle (connect / disconnect)
 *
 * Auto-connects when exactly one host is found; shows [DiscoveryUiState.ServiceSelection]
 * when two or more hosts appear; falls back to [DiscoveryUiState.NothingFound] after
 * [scanTimeoutMs] of zero results.
 */
class DiscoveryViewModel(
    private val observeHosts: FlowUseCase<NoParams, List<DiscoveredHost>>,
    private val connectionManager: ConnectionManager,
    private val networkMonitor: INetworkMonitor,
    private val dispatchers: AppDispatchers,
    private val state: StateCommunication.Mutable<DiscoveryUiState> = StateCommunication(
        DiscoveryUiState.Scanning,
    ),
    private val events: EventCommunication.Mutable<DiscoveryEvent> = EventCommunication(),
    private val scanTimeoutMs: Long = SCAN_TIMEOUT_MS,
    private val navigateDelayMs: Long = NAVIGATE_DELAY_MS,
) : ViewModel() {

    val uiState: StateFlow<DiscoveryUiState> = state.observe
    val uiEvents: Flow<DiscoveryEvent> = events.observe

    private var discoveryJob: Job? = null
    private var connectionJob: Job? = null

    init {
        observeNetwork()
    }

    fun selectHost(host: DiscoveredHost) {
        connectToHost(host)
    }

    fun retry() {
        startDiscovery()
    }

    fun cancel() {
        cancelAll()
        state.put(DiscoveryUiState.Idle)
    }

    // region internal machinery

    private fun observeNetwork() {
        dispatchers.launchBackground(viewModelScope) {
            networkMonitor.isNetworkAvailable.collect { available ->
                if (available) {
                    startDiscovery()
                } else {
                    cancelAll()
                    state.put(DiscoveryUiState.NetworkUnavailable)
                }
            }
        }
    }

    private fun startDiscovery() {
        cancelAll()
        state.put(DiscoveryUiState.Scanning)

        discoveryJob = dispatchers.launchBackground(viewModelScope) {
            val timeoutJob = launch {
                delay(scanTimeoutMs)
                if (state.observe.value is DiscoveryUiState.Scanning) {
                    state.put(DiscoveryUiState.NothingFound)
                }
            }

            observeHosts(NoParams)
                .catch { e ->
                    timeoutJob.cancel()
                    state.put(DiscoveryUiState.ScanError(e.message.orEmpty()))
                }
                .collect { hosts -> onHostsReceived(hosts, timeoutJob) }
        }
    }

    private fun onHostsReceived(hosts: List<DiscoveredHost>, timeoutJob: Job) {
        when (state.observe.value) {
            is DiscoveryUiState.Scanning,
            is DiscoveryUiState.NothingFound       -> {
                when {
                    hosts.isEmpty() -> Unit
                    hosts.size == 1 -> {
                        timeoutJob.cancel()
                        connectToHost(hosts.first())
                    }

                    else            -> {
                        timeoutJob.cancel()
                        state.put(DiscoveryUiState.ServiceSelection(hosts))
                    }
                }
            }

            is DiscoveryUiState.ServiceSelection   -> {
                if (hosts.isEmpty()) {
                    state.put(DiscoveryUiState.NothingFound)
                } else {
                    state.put(DiscoveryUiState.ServiceSelection(hosts))
                }
            }

            is DiscoveryUiState.Idle,
            is DiscoveryUiState.Connecting,
            is DiscoveryUiState.Connected,
            is DiscoveryUiState.ConnectionFailed,
            is DiscoveryUiState.ScanError,
            is DiscoveryUiState.NetworkUnavailable -> Unit
        }
    }

    private fun connectToHost(host: DiscoveredHost) {
        discoveryJob?.cancel()
        discoveryJob = null
        connectionJob?.cancel()

        state.put(DiscoveryUiState.Connecting(host))

        connectionJob = dispatchers.launchBackground(viewModelScope) {
            try {
                connectionManager.connect(host)
                connectionManager.state
                    .onEach { connectionState -> handleConnectionState(connectionState) }
                    .launchIn(this)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.put(
                    DiscoveryUiState.ConnectionFailed(
                        hostResource = TextResource.RawText(host.name),
                        reasonResource = TextResource.ResText(Res.string.error_connection_failed),
                        host = host,
                    ),
                )
            }
        }
    }

    private suspend fun handleConnectionState(connectionState: ConnectionState) {
        when (connectionState) {
            is ConnectionState.Connected -> {
                state.put(DiscoveryUiState.Connected(connectionState.host))
                delay(navigateDelayMs)
                events.send(DiscoveryEvent.NavigateToBuild)
            }

            is ConnectionState.Failed    -> {
                state.put(
                    DiscoveryUiState.ConnectionFailed(
                        TextResource.RawText(connectionState.host.name),
                        formatErrorReason(connectionState.reason),
                        connectionState.host,
                    ),
                )
            }

            is ConnectionState.Connecting,
            is ConnectionState.Reconnecting,
            is ConnectionState.Idle,
            ConnectionState.Disconnected -> Unit
        }
    }

    private fun cancelAll() {
        discoveryJob?.cancel()
        discoveryJob = null
        connectionJob?.cancel()
        connectionJob = null
    }

    override fun onCleared() {
        cancelAll()
    }

    companion object {
        private const val SCAN_TIMEOUT_MS = 10_000L
        private const val NAVIGATE_DELAY_MS = 800L
    }
}

private fun formatErrorReason(reason: ConnectionErrorReason): TextResource {
    val resource = when (reason) {
        is ConnectionErrorReason.Timeout         -> Res.string.error_timeout
        is ConnectionErrorReason.Refused         -> Res.string.error_refused
        is ConnectionErrorReason.HandshakeFailed -> Res.string.error_handshake
        is ConnectionErrorReason.Lost            -> Res.string.error_lost
        is ConnectionErrorReason.Unknown         -> Res.string.error_unknown
    }
    return TextResource.ResText(resource, reason.message)
}
