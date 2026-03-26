package me.yuriisoft.buildnotify.mobile.network.connection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.mapper.Mapper
import me.yuriisoft.buildnotify.mobile.data.protocol.HeartbeatPayload
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.protocol.WsPayload
import me.yuriisoft.buildnotify.mobile.network.error.ConnectionErrorReason
import me.yuriisoft.buildnotify.mobile.network.reconnection.ReconnectionStrategy
import me.yuriisoft.buildnotify.mobile.network.transport.Transport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class ManagedConnectionTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = AppDispatchers.Abstract(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher,
    )

    private val host = DiscoveredHost(name = "Test", host = "10.0.0.1", port = 8765)

    private class StubErrorMapping : Mapper<Throwable, ConnectionErrorReason> {
        override fun map(from: Throwable): ConnectionErrorReason =
            ConnectionErrorReason.Unknown(from.message ?: "unknown")
    }

    private class NeverRetry : ReconnectionStrategy {
        override suspend fun shouldRetry(cause: Throwable, attempt: Long): Boolean = false
    }

    private class ControllableTransport : Transport {
        val payloads = MutableSharedFlow<WsPayload>(extraBufferCapacity = 64)
        var openCount = 0
            private set

        override fun open(
            host: String,
            port: Int,
            outgoing: ReceiveChannel<WsEnvelope>,
        ): Flow<WsPayload> = flow {
            openCount++
            payloads.collect { emit(it) }
        }
    }

    private fun createConnection(
        transport: Transport = ControllableTransport(),
        reconnection: ReconnectionStrategy = NeverRetry(),
        errorMapping: Mapper<Throwable, ConnectionErrorReason> = StubErrorMapping(),
    ) = ManagedConnection(
        transport = transport,
        reconnection = reconnection,
        errorMapping = errorMapping,
        heartbeatTimeout = 1.hours,
        dispatchers = dispatchers,
    )

    // region State transitions

    @Test
    fun initialStateIsIdle() {
        val connection = createConnection()

        assertIs<ConnectionState.Idle>(connection.state.value)
    }

    @Test
    fun connectTransitionsToConnecting() = runTest(testDispatcher) {
        val transport = ControllableTransport()
        val connection = createConnection(transport = transport)

        connection.connect(host)
        advanceUntilIdle()

        val state = connection.state.value
        val isConnectingOrConnected =
            state is ConnectionState.Connecting || state is ConnectionState.Connected
        assert(isConnectingOrConnected) {
            "Expected Connecting or Connected, got $state"
        }
    }

    @Test
    fun transitionsToConnectedOnFirstPayload() = runTest(testDispatcher) {
        val transport = ControllableTransport()
        val connection = createConnection(transport = transport)

        connection.connect(host)
        advanceUntilIdle()

        transport.payloads.emit(HeartbeatPayload())
        advanceUntilIdle()

        assertIs<ConnectionState.Connected>(connection.state.value)
    }

    @Test
    fun disconnectTransitionsToDisconnected() = runTest(testDispatcher) {
        val transport = ControllableTransport()
        val connection = createConnection(transport = transport)

        connection.connect(host)
        advanceUntilIdle()

        connection.disconnect()
        advanceUntilIdle()

        assertIs<ConnectionState.Disconnected>(connection.state.value)
    }

    // endregion

    // region Data plane

    @Test
    fun incomingPayloadsAreSharedViaFlow() = runTest(testDispatcher) {
        val transport = ControllableTransport()
        val connection = createConnection(transport = transport)

        connection.connect(host)
        advanceUntilIdle()

        val payload = HeartbeatPayload()
        transport.payloads.emit(payload)

        val received = connection.incoming.first()
        assertEquals(payload, received)
    }

    // endregion

    // region Error handling

    @Test
    fun transitionsToFailedWhenTransportErrorsAndNoRetry() = runTest(testDispatcher) {
        val failingTransport = Transport { _, _, _ ->
            flow { throw RuntimeException("connection lost") }
        }
        val connection = createConnection(transport = failingTransport)

        connection.connect(host)
        advanceUntilIdle()

        val state = connection.state.value
        assertIs<ConnectionState.Failed>(state)
        assertIs<ConnectionErrorReason.Unknown>(state.reason)
    }

    @Test
    fun reconnectsWhenStrategyReturnsTrue() = runTest(testDispatcher) {
        var attempts = 0
        val transport = Transport { _, _, _ ->
            flow {
                attempts++
                if (attempts <= 2) throw RuntimeException("fail #$attempts")
                // Third attempt: emit a payload and stay open
                emit(HeartbeatPayload())
                kotlinx.coroutines.awaitCancellation()
            }
        }
        val retryTwice = object : ReconnectionStrategy {
            override suspend fun shouldRetry(cause: Throwable, attempt: Long): Boolean =
                attempt < 2
        }
        val connection = createConnection(transport = transport, reconnection = retryTwice)

        connection.connect(host)
        advanceUntilIdle()

        assertIs<ConnectionState.Connected>(connection.state.value)
        assertEquals(3, attempts)
    }

    // endregion

    // region Reconnect after disconnect

    @Test
    fun canReconnectAfterDisconnect() = runTest(testDispatcher) {
        val transport = ControllableTransport()
        val connection = createConnection(transport = transport)

        connection.connect(host)
        advanceUntilIdle()
        connection.disconnect()
        advanceUntilIdle()

        assertIs<ConnectionState.Disconnected>(connection.state.value)

        connection.connect(host)
        advanceUntilIdle()

        transport.payloads.emit(HeartbeatPayload())
        advanceUntilIdle()

        assertEquals(2, transport.openCount)
    }

    // endregion
}
