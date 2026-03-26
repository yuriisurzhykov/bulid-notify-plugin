package me.yuriisoft.buildnotify.mobile.network.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import me.yuriisoft.buildnotify.mobile.core.communication.StateCommunication
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.mapper.Mapper
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.protocol.WsPayload
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.reconnection.ReconnectionStrategy
import me.yuriisoft.buildnotify.mobile.network.transport.Transport
import kotlin.time.Duration

/**
 * Composable Flow pipeline that implements both [ActiveSession] (data plane)
 * and [ConnectionManager] (control plane), satisfying ISP.
 *
 * The entire connection lifecycle reads as a single pipeline:
 * "Open transport. On each payload, track connected state and share it.
 * Time out if no messages (heartbeat missed). On error, reconnect with
 * backoff. When done, report final state."
 *
 * No `while(true)`. No manual `delay(3000)`. No mutable session field.
 * `retryWhen` restarts the upstream automatically; `timeout` replaces a
 * manual heartbeat monitor; `onCompletion` handles all terminal transitions.
 *
 * Sending uses the actor pattern ([Channel]), eliminating mutexes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManagedConnection(
    private val transport: Transport,
    private val reconnection: ReconnectionStrategy,
    private val errorMapping: Mapper<Throwable, ConnectionErrorReason>,
    private val heartbeatTimeout: Duration,
    private val dispatchers: AppDispatchers,
) : ActiveSession, ConnectionManager {

    private val _state: StateCommunication.Mutable<ConnectionState> =
        StateCommunication(ConnectionState.Idle as ConnectionState)

    override val state: StateFlow<ConnectionState> = _state.observe

    private val _incoming = MutableSharedFlow<WsPayload>(extraBufferCapacity = INCOMING_BUFFER)

    override val incoming: SharedFlow<WsPayload> = _incoming.asSharedFlow()

    private val outgoing = Channel<WsEnvelope>(Channel.BUFFERED)

    override suspend fun send(envelope: WsEnvelope) {
        outgoing.send(envelope)
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val host = state.value.hostOrNull() ?: return@CoroutineExceptionHandler
        _state.put(ConnectionState.Failed(host, errorMapping.map(throwable)))
    }

    private val scope = CoroutineScope(
        dispatchers.io + SupervisorJob() + exceptionHandler
    )

    private var pipeline: Job? = null

    @OptIn(FlowPreview::class)
    override suspend fun connect(host: DiscoveredHost) {
        pipeline?.cancelAndJoin()
        _state.put(ConnectionState.Connecting(host))

        pipeline = transport.open(host.host, host.port, outgoing)
            .onEach { payload ->
                if (state.value !is ConnectionState.Connected) {
                    _state.put(ConnectionState.Connected(host))
                }
                _incoming.emit(payload)
            }
            .timeout(heartbeatTimeout)
            .retryWhen { cause, attempt ->
                _state.put(ConnectionState.Reconnecting(host, attempt + 1))
                reconnection.shouldRetry(cause, attempt)
            }
            .onCompletion { cause ->
                when {
                    cause == null                   -> _state.put(ConnectionState.Disconnected)
                    cause !is CancellationException ->
                        _state.put(ConnectionState.Failed(host, errorMapping.map(cause)))
                }
            }
            .launchIn(scope)
    }

    override suspend fun disconnect() {
        pipeline?.cancelAndJoin()
        pipeline = null
        _state.put(ConnectionState.Disconnected)
    }

    private companion object {
        const val INCOMING_BUFFER = 64
    }
}

private fun ConnectionState.hostOrNull(): DiscoveredHost? = when (this) {
    is ConnectionState.Connecting   -> host
    is ConnectionState.Connected    -> host
    is ConnectionState.Reconnecting -> host
    is ConnectionState.Failed       -> host

    is ConnectionState.Idle,
    is ConnectionState.Disconnected -> null
}
