package me.yuriisoft.buildnotify.mobile.feature.discovery.domain

import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.core.usecase.FlowUseCase
import me.yuriisoft.buildnotify.mobile.core.usecase.NoParams
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.INsdRepository
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost

@Inject
class ObserveHostsUseCase(
    private val repository: INsdRepository,
) : FlowUseCase.Abstract<NoParams, List<DiscoveredHost>>() {

    override fun execute(params: NoParams): Flow<List<DiscoveredHost>> =
        repository.discoverHosts()
}