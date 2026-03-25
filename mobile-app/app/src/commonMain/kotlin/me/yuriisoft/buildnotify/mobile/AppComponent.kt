package me.yuriisoft.buildnotify.mobile

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.data.discovery.INsdDiscovery
import me.yuriisoft.buildnotify.mobile.data.repository.NsdRepository
import me.yuriisoft.buildnotify.mobile.domain.repository.INsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.DiscoveryScreen
import me.yuriisoft.buildnotify.mobile.feature.discovery.DiscoveryViewModel
import me.yuriisoft.buildnotify.mobile.feature.discovery.ObserveHostsUseCase

/**
 * Composition Root — the only place in the project that sees every module.
 *
 * Platform entry points pass the platform-specific [INsdDiscovery] via
 * the constructor and retrieve the ready-to-use [screens] set.
 *
 * When new features are added, register their Screen via an
 * additional [@IntoSet][IntoSet] provider and supply any missing
 * ViewModel / use-case bindings below.
 */
@Component
abstract class AppComponent(
    @get:Provides protected val nsdDiscovery: INsdDiscovery,
) {

    abstract val screens: Set<Screen>

    protected val NsdRepository.bind: INsdRepository
        @Provides get() = this

    @Provides
    protected fun dispatchers(): AppDispatchers = AppDispatchers.Default()

    @Provides
    protected fun discoveryViewModel(
        observeHosts: ObserveHostsUseCase,
        dispatchers: AppDispatchers,
    ): DiscoveryViewModel = DiscoveryViewModel(observeHosts, dispatchers)

    @Provides
    @IntoSet
    protected fun discoveryScreen(screen: DiscoveryScreen): Screen = screen

    companion object
}
