package me.yuriisoft.buildnotify.mobile.feature.discovery.data.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.data.session.BuildSession
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionStatus
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.IConnectionRepository

/**
 * Application-scoped singleton that owns the WebSocket connection lifecycle.
 *
 * Responsibilities:
 *   - Opens / closes the WebSocket via [BuildSession]
 *   - Emits [ConnectionStatus] as the single source of truth for the UI
 *   - Automatically reconnects on failure via [ReconnectionPolicy]
 *
 * The internal coroutine scope survives screen navigation; only an explicit
 * [disconnect] (or process death) tears down the connection.
 */
@Inject
class ConnectionManager(
    private val session: BuildSession,
    private val dispatchers: AppDispatchers,
    private val reconnectionPolicy: ReconnectionPolicy,
) : IConnectionRepository {

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    private val mutex = Mutex()
    private var connectionJob: Job? = null

    override suspend fun connect(host: DiscoveredHost): Unit = mutex.withLock {
        cancelConnection()
        reconnectionPolicy.reset()
        connectionJob = scope.launch { retryLoop(host) }
    }

    override suspend fun disconnect(): Unit = mutex.withLock {
        cancelConnection()
        _status.value = ConnectionStatus.Disconnected
    }

    private suspend fun cancelConnection() {
        connectionJob?.cancelAndJoin()
        connectionJob = null
    }

    /**
     * Connect -> collect -> on error check policy -> delay -> repeat.
     * Exits when [ReconnectionPolicy] is exhausted or the job is cancelled.
     */
    private suspend fun retryLoop(host: DiscoveredHost) {
        while (true) {
            _status.value = ConnectionStatus.Connecting(host)
            delay(3000)
            try {
                collectSession(host)
                _status.value = ConnectionStatus.Error(
                    host,
                    ConnectionErrorReason.Lost("Server closed the connection"),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _status.value = ConnectionStatus.Error(host, e.toErrorReason())
            }
            if (!reconnectionPolicy.awaitNextAttempt()) break
        }
    }

    /**
     * Collects the WebSocket payload flow.
     * Transitions to [ConnectionStatus.Connected] on the first received payload
     * (the server sends a handshake immediately after the WebSocket opens).
     */
    private suspend fun collectSession(host: DiscoveredHost) {
        var connected = false
        session.connect(host.host, host.port).collect { _ ->
            if (!connected) {
                connected = true
                reconnectionPolicy.reset()
                _status.value = ConnectionStatus.Connected(host)
            }
        }
    }
}

/**
 * Maps a connection-layer [Throwable] to a domain [ConnectionErrorReason].
 *
 * Uses class-name and message heuristics because Ktor wraps platform exceptions
 * differently on each target (JVM, iOS). The classified reason drives UI error
 * messages without requiring the presentation layer to inspect raw exceptions.
 */
internal fun Throwable.toErrorReason(): ConnectionErrorReason {
    val msg = message ?: "Connection failed"
    val className = this::class.simpleName.orEmpty()
    return when {
        className.contains("Timeout", ignoreCase = true) ->
            ConnectionErrorReason.Timeout(0L)

        className.contains("Connect", ignoreCase = true) ||
                msg.contains("refused", ignoreCase = true) ->
            ConnectionErrorReason.Refused(msg)

        msg.contains("handshake", ignoreCase = true) ||
                msg.contains("upgrade", ignoreCase = true) ->
            ConnectionErrorReason.HandshakeFailed(msg)

        msg.contains("closed", ignoreCase = true) ||
                msg.contains("reset", ignoreCase = true) ||
                msg.contains("broken pipe", ignoreCase = true) ->
            ConnectionErrorReason.Lost(msg)

        else -> ConnectionErrorReason.Unknown(msg)
    }
}
