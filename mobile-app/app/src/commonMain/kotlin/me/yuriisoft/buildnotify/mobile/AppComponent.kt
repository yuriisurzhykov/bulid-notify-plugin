package me.yuriisoft.buildnotify.mobile

import com.yuriisurzhykov.buildnotifier.feature.catalog.CatalogScreen
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.core.platform.AppVersionProvider
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.connection.ReconnectionPolicy
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.INsdDiscovery
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.NsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.ObserveHostsUseCase
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.IConnectionRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.INsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryScreen
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryViewModel

/**
 * Composition Root — the only place in the project that sees every module.
 *
 * Platform entry points pass the platform-specific [INsdDiscovery] and
 * [INetworkMonitor] via the constructor and retrieve the ready-to-use
 * [screens] set.
 *
 * When new features are added, register their Screen via an
 * additional [@IntoSet][IntoSet] provider and supply any missing
 * ViewModel / use-case bindings below.
 */
@Component
abstract class AppComponent(
    @get:Provides protected val nsdDiscovery: INsdDiscovery,
    @get:Provides protected val networkMonitor: INetworkMonitor,
    @get:Provides val appVersionProvider: AppVersionProvider,
) {

    abstract val screens: Set<Screen>

    abstract val connectionRepository: IConnectionRepository

    protected val NsdRepository.bind: INsdRepository
        @Provides get() = this

    protected val ConnectionManager.bind: IConnectionRepository
        @Provides get() = this

    @Provides
    protected fun dispatchers(): AppDispatchers = AppDispatchers.Default()

    @Provides
    protected fun httpClient(): HttpClient = HttpClient { install(WebSockets) }

    @Provides
    protected fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    protected fun reconnectionPolicy(): ReconnectionPolicy = ReconnectionPolicy()

    @Provides
    protected fun discoveryViewModel(
        observeHosts: ObserveHostsUseCase,
        connectionRepository: IConnectionRepository,
        networkMonitor: INetworkMonitor,
        dispatchers: AppDispatchers,
    ): DiscoveryViewModel = DiscoveryViewModel(
        observeHosts = observeHosts,
        connectionRepository = connectionRepository,
        networkMonitor = networkMonitor,
        dispatchers = dispatchers,
    )

    @IntoSet
    @Provides
    protected fun discoveryScreen(screen: DiscoveryScreen): Screen = screen

    @IntoSet
    @Provides
    protected fun catalogScreen(screen: CatalogScreen): Screen = screen

    companion object
}
