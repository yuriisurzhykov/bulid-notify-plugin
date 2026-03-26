package me.yuriisoft.buildnotify.mobile.feature.discovery.presentation

import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource

sealed interface DiscoveryUiState {

    data object Idle : DiscoveryUiState

    data object Scanning : DiscoveryUiState

    data class ServiceSelection(val hosts: List<DiscoveredHost>) : DiscoveryUiState

    data class Connecting(val host: DiscoveredHost) : DiscoveryUiState

    data class Connected(val host: DiscoveredHost) : DiscoveryUiState

    data class ConnectionFailed(
        val host: DiscoveredHost,
        val reason: TextResource,
    ) : DiscoveryUiState

    data object NothingFound : DiscoveryUiState

    data class ScanError(val message: String) : DiscoveryUiState

    data object NetworkUnavailable : DiscoveryUiState
}
