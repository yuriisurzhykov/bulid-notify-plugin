package me.yuriisoft.buildnotify.mobile.feature.discovery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.error_client_rejected
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
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.core.communication.EventCommunication
import me.yuriisoft.buildnotify.mobile.core.communication.StateCommunication
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor
import me.yuriisoft.buildnotify.mobile.core.usecase.NoParams
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.ObserveHostsUseCase
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionState
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.tls.TrustedServers
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource

/**
 * ViewModel for the discovery / connection flow.
 *
 * Drives [DiscoveryUiState] transitions and fires one-shot [DiscoveryEvent]s.
 *
 * ### Phase 5 change
 * [formatErrorReason] now handles [ConnectionErrorReason.ClientRejected] with a
 * dedicated user-facing string (`error_client_rejected`) that guides the user to
 * re-pair via IDE settings. All other branches are unchanged.
 *
 * The retry button visible in [DiscoveryUiState.ConnectionFailed] is intentionally
 * **still shown** even for `ClientRejected` — it lets the user try again after
 * they have cleared the rejection in the plugin settings, without having to
 * restart the app.
 */
@Inject
class DiscoveryViewModel(
    private val observeHosts: ObserveHostsUseCase,
    private val connectionManager: ConnectionManager,
    private val networkMonitor: INetworkMonitor,
    private val trustedServers: TrustedServers,
    private val dispatchers: AppDispatchers,
    private val state: StateCommunication.Mutable<DiscoveryUiState> = StateCommunication(
        DiscoveryUiState.Idle,
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
        when {
            host.isSecure && !trustedServers.isPinned(host.trustKey) -> {
                val pairingConfirmation = DiscoveryUiState.PairingConfirmation(
                    host = host,
                    fingerprint = host.fingerprint.orEmpty()
                )
                state.put(pairingConfirmation)
            }

            else                                                     -> connectToHost(host)
        }
    }

    fun confirmPairing() {
        val current = state.observe.value as? DiscoveryUiState.PairingConfirmation ?: return
        trustedServers.pin(current.host.trustKey, current.host.fingerprint ?: return)
        connectToHost(current.host)
    }

    fun rejectPairing() {
        state.put(DiscoveryUiState.Idle)
    }

    fun retry() {
        startDiscovery()
    }

    fun cancel() {
        cancelAll()
        state.put(DiscoveryUiState.Idle)
    }

    private fun observeNetwork() {
        dispatchers.launchBackground(viewModelScope) {
            networkMonitor.isNetworkAvailable.collect { available ->
                if (!available) {
                    cancelAll()
                    connectionManager.disconnect()
                    state.update { DiscoveryUiState.NetworkUnavailable }
                } else {
                    state.update { DiscoveryUiState.Idle }
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
                    discoveryJob?.cancel()
                    state.put(DiscoveryUiState.NothingFound)
                }
            }

            observeHosts.invoke(NoParams)
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
            is DiscoveryUiState.NothingFound       -> when {
                hosts.isEmpty() -> Unit
                hosts.size == 1 -> {
                    timeoutJob.cancel()
                    selectHost(hosts.first())
                }

                else            -> {
                    timeoutJob.cancel()
                    state.put(DiscoveryUiState.ServiceSelection(hosts))
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
            is DiscoveryUiState.PairingConfirmation,
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
            } catch (_: Exception) {
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
            is ConnectionState.Connected    -> handleConnectedState(connectionState)

            is ConnectionState.Failed       -> {
                connectionManager.disconnect()
                state.put(
                    DiscoveryUiState.ConnectionFailed(
                        hostResource = TextResource.RawText(connectionState.host.name),
                        reasonResource = formatErrorReason(connectionState.reason),
                        host = connectionState.host,
                    ),
                )
            }

            is ConnectionState.Connecting,
            is ConnectionState.Reconnecting,
            is ConnectionState.Idle,
            is ConnectionState.Disconnected -> Unit
        }
    }

    private suspend fun handleConnectedState(connectionState: ConnectionState.Connected) {
        state.put(DiscoveryUiState.Connected(connectionState.host))
        if (navigateDelayMs > 0) delay(navigateDelayMs)
        events.send(DiscoveryEvent.NavigateToBuild)
    }

    private fun cancelAll() {
        discoveryJob?.cancel()
        discoveryJob = null
        connectionJob?.cancel()
        connectionJob = null
    }

    override fun onCleared() {
        cancelAll()
        viewModelScope.launch { connectionManager.disconnect() }
    }

    companion object {
        private const val SCAN_TIMEOUT_MS = 10_000L
        private const val NAVIGATE_DELAY_MS = 300L
    }
}

/**
 * Maps a typed [ConnectionErrorReason] to a [TextResource] for display in
 * [DiscoveryUiState.ConnectionFailed].
 *
 * The [ConnectionErrorReason.message] field is **not** surfaced for
 * `ClientRejected` because the raw exception message is a technical string
 * (`"Client explicitly rejected"`) not suitable for end users. The resource
 * string is used instead.
 */
private fun formatErrorReason(reason: ConnectionErrorReason): TextResource {
    val resource = when (reason) {
        is ConnectionErrorReason.Timeout         -> Res.string.error_timeout
        is ConnectionErrorReason.Refused         -> Res.string.error_refused
        is ConnectionErrorReason.HandshakeFailed -> Res.string.error_handshake
        is ConnectionErrorReason.Lost            -> Res.string.error_lost
        is ConnectionErrorReason.ClientRejected  -> Res.string.error_client_rejected
        is ConnectionErrorReason.Unknown         -> Res.string.error_unknown
    }
    return when (reason) {
        is ConnectionErrorReason.ClientRejected -> TextResource.ResText(resource)
        else                                    -> TextResource.ResText(resource, reason.message)
    }
}