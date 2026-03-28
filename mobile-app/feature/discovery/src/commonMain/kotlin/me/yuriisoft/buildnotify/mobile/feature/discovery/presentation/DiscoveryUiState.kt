package me.yuriisoft.buildnotify.mobile.feature.discovery.presentation

import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource

sealed interface DiscoveryUiState {

    val animateOrder: Int

    data object Idle : DiscoveryUiState {
        override val animateOrder: Int = 0
    }

    data object Scanning : DiscoveryUiState {
        override val animateOrder: Int = 1
    }

    data class ServiceSelection(val hosts: List<DiscoveredHost>) : DiscoveryUiState {
        override val animateOrder: Int = 2
    }

    data class PairingConfirmation(
        val host: DiscoveredHost,
        val fingerprint: String,
    ) : DiscoveryUiState {
        override val animateOrder: Int = 3
    }

    data class Connecting(val host: DiscoveredHost) : DiscoveryUiState {
        override val animateOrder: Int = 4
    }

    data class Connected(val host: DiscoveredHost) : DiscoveryUiState {
        override val animateOrder: Int = 5
    }

    data class ConnectionFailed(
        val hostResource: TextResource,
        val reasonResource: TextResource,
        val host: DiscoveredHost,
    ) : DiscoveryUiState {
        override val animateOrder: Int = 5
    }

    data object NothingFound : DiscoveryUiState {
        override val animateOrder: Int = 2
    }

    data class ScanError(val message: String) : DiscoveryUiState {
        override val animateOrder: Int = 2
    }

    data object NetworkUnavailable : DiscoveryUiState {
        override val animateOrder: Int = -1
    }
}
