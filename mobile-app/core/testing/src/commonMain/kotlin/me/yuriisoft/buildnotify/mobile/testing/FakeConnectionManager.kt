package me.yuriisoft.buildnotify.mobile.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionState
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost

/**
 * In-memory fake of [ConnectionManager] for unit tests.
 *
 * Call [emitState] to push connection state changes.
 * All [connect] and [disconnect] calls are recorded for assertion.
 */
class FakeConnectionManager : ConnectionManager {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    val connectCalls: MutableList<DiscoveredHost> = mutableListOf()
    var disconnectCalls: Int = 0
        private set

    override suspend fun connect(host: DiscoveredHost) {
        connectCalls += host
        _state.value = ConnectionState.Connected(host)
    }

    override suspend fun disconnect() {
        disconnectCalls++
        _state.value = ConnectionState.Disconnected
    }

    fun emitState(state: ConnectionState) {
        _state.value = state
    }
}
