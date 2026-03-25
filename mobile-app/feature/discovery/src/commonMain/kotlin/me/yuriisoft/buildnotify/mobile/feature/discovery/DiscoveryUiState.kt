package me.yuriisoft.buildnotify.mobile.feature.discovery

import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

sealed interface DiscoveryUiState {

    data object Loading : DiscoveryUiState

    data class Content(val hosts: List<DiscoveredHost>) : DiscoveryUiState

    data class Error(val message: String) : DiscoveryUiState
}
