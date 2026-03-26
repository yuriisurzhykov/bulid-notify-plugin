package me.yuriisoft.buildnotify.mobile.feature.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.INsdRepository
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost

/**
 * In-memory fake of [INsdRepository] for unit tests.
 *
 * Call [emit] to update the list of discovered hosts.
 * The initial state is an empty list.
 */
class FakeNsdRepository : INsdRepository {

    private val _hosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())

    override fun discoverHosts(): Flow<List<DiscoveredHost>> =
        _hosts.asStateFlow()

    fun emit(hosts: List<DiscoveredHost>) {
        _hosts.value = hosts
    }
}
