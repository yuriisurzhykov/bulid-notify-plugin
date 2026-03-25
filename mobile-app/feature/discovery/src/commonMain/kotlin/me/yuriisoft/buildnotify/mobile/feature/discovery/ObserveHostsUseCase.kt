package me.yuriisoft.buildnotify.mobile.feature.discovery

import kotlinx.coroutines.flow.Flow
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.core.usecase.FlowUseCase
import me.yuriisoft.buildnotify.mobile.core.usecase.NoParams
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.domain.repository.INsdRepository

@Inject
class ObserveHostsUseCase(
    private val repository: INsdRepository,
) : FlowUseCase.Abstract<NoParams, List<DiscoveredHost>>() {

    override fun execute(params: NoParams): Flow<List<DiscoveredHost>> =
        repository.discoverHosts()
}
