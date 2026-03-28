package me.yuriisoft.buildnotify.mobile.feature.discovery.di

import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.NsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.ObserveHostsUseCase
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.INsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryScreen
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryViewModel
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.tls.TrustedServers

/**
 * Contribution interface for the Discovery feature.
 *
 * Declares everything this feature provides to the DI graph
 * and implicitly requires from the parent component through
 * abstract property references.
 *
 * The parent [AppComponent] inherits this interface — kotlin-inject
 * sees the [@Provides] methods and wires them automatically.
 * The feature module never sees the parent; the parent never sees
 * the feature's internal wiring. OCP + DIP.
 */
interface DiscoveryComponent {

    // --- What the feature NEEDS (satisfied by parent) ---
    // kotlin-inject resolves these from the parent graph.
    // No explicit declaration needed — they're already in AppComponent.

    // --- What the feature PROVIDES ---
    @Provides
    fun NsdRepository.bind(): INsdRepository = this

    @Provides
    fun discoveryViewModel(
        observeHosts: ObserveHostsUseCase,
        connectionManager: ConnectionManager,
        networkMonitor: INetworkMonitor,
        trustedServers: TrustedServers,
        dispatchers: AppDispatchers,
    ): DiscoveryViewModel = DiscoveryViewModel(
        observeHosts = observeHosts,
        connectionManager = connectionManager,
        networkMonitor = networkMonitor,
        trustedServers = trustedServers,
        dispatchers = dispatchers,
    )

    @IntoSet
    @Provides
    fun discoveryScreen(screen: DiscoveryScreen): Screen = screen
}