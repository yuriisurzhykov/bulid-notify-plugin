package me.yuriisoft.buildnotify.mobile.navigation

import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.core.navigation.Destination
import me.yuriisoft.buildnotify.mobile.core.navigation.StartRoute
import me.yuriisoft.buildnotify.mobile.core.navigation.routes.BuildStatusDestination
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryDestination
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionState

@Inject
class ConnectionAwareStartRoute(
    private val connectivityManager: ConnectionManager
) : StartRoute {

    override fun resolve(): Destination = when (connectivityManager.state.value) {
        is ConnectionState.Connected -> BuildStatusDestination
        else                         -> DiscoveryDestination
    }
}