package me.yuriisoft.buildnotify.mobile.feature.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionStatus
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.IConnectionRepository

/**
 * In-memory fake of [IConnectionRepository] for unit tests.
 *
 * Call [emitStatus] to push connection state changes.
 * All [connect] and [disconnect] calls are recorded for assertion.
 */
class FakeConnectionRepository : IConnectionRepository {

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    val connectCalls: MutableList<DiscoveredHost> = mutableListOf()
    var disconnectCalls: Int = 0
        private set

    override suspend fun connect(host: DiscoveredHost) {
        connectCalls += host
        _status.value = ConnectionStatus.Connected(host)
    }

    override suspend fun disconnect() {
        disconnectCalls++
        _status.value = ConnectionStatus.Disconnected
    }

    fun emitStatus(status: ConnectionStatus) {
        _status.value = status
    }
}
