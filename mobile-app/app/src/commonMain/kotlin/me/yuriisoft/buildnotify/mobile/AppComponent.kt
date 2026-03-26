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
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.INsdDiscovery
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.NsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.ObserveHostsUseCase
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.INsdRepository
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryScreen
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryViewModel
import me.yuriisoft.buildnotify.mobile.network.client.HttpClientProvider
import me.yuriisoft.buildnotify.mobile.network.connection.ActiveSession
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ManagedConnection
import me.yuriisoft.buildnotify.mobile.network.error.ErrorMapping
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.HandshakeErrors
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.LostConnectionErrors
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.RefusedErrors
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.TimeoutErrors
import me.yuriisoft.buildnotify.mobile.network.reconnection.ExponentialBackoff
import me.yuriisoft.buildnotify.mobile.network.reconnection.ReconnectionStrategy
import me.yuriisoft.buildnotify.mobile.network.transport.PayloadCodec
import me.yuriisoft.buildnotify.mobile.network.transport.Transport
import me.yuriisoft.buildnotify.mobile.network.transport.WebSocketTransport
import kotlin.time.Duration.Companion.seconds

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
@AppScope
@Component
abstract class AppComponent(
    @get:Provides protected val nsdDiscovery: INsdDiscovery,
    @get:Provides protected val networkMonitor: INetworkMonitor,
    @get:Provides val appVersionProvider: AppVersionProvider,
) {

    abstract val screens: Set<Screen>

    abstract val connectionManager: ConnectionManager

    // --- NSD discovery wiring ---

    protected val NsdRepository.bind: INsdRepository
        @Provides get() = this

    @Provides
    protected fun dispatchers(): AppDispatchers = AppDispatchers.Default()

    @Provides
    protected fun httpClient(): HttpClient = HttpClient { install(WebSockets) }

    @Provides
    protected fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    protected fun discoveryViewModel(
        observeHosts: ObserveHostsUseCase,
        connectionManager: ConnectionManager,
        networkMonitor: INetworkMonitor,
        dispatchers: AppDispatchers,
    ): DiscoveryViewModel = DiscoveryViewModel(
        observeHosts = observeHosts,
        connectionManager = connectionManager,
        networkMonitor = networkMonitor,
        dispatchers = dispatchers,
    )

    @IntoSet
    @Provides
    protected fun discoveryScreen(screen: DiscoveryScreen): Screen = screen

    @IntoSet
    @Provides
    protected fun catalogScreen(screen: CatalogScreen): Screen = screen

    // --- :core:network wiring (ISP: ManagedConnection → ActiveSession + ConnectionManager) ---

    @Provides
    protected fun payloadCodec(json: Json): PayloadCodec = PayloadCodec(json)

    @Provides
    protected fun transport(codec: PayloadCodec): Transport =
        WebSocketTransport(HttpClientProvider().provide(), codec)

    @Provides
    protected fun reconnectionStrategy(): ReconnectionStrategy = ExponentialBackoff()

    @Provides
    protected fun errorRecognizers(): List<ErrorRecognizer> = listOf(
        TimeoutErrors(),
        RefusedErrors(),
        HandshakeErrors(),
        LostConnectionErrors(),
    )

    @Provides
    protected fun errorMapping(recognizers: List<ErrorRecognizer>): ErrorMapping =
        ErrorMapping(recognizers)

    @AppScope
    @Provides
    protected fun managedConnection(
        transport: Transport,
        reconnection: ReconnectionStrategy,
        errorMapping: ErrorMapping,
        dispatchers: AppDispatchers,
    ): ManagedConnection = ManagedConnection(
        transport = transport,
        reconnection = reconnection,
        errorMapping = errorMapping,
        heartbeatTimeout = HEARTBEAT_TIMEOUT,
        dispatchers = dispatchers,
    )

    protected val ManagedConnection.bindSession: ActiveSession
        @Provides get() = this

    protected val ManagedConnection.bindManager: ConnectionManager
        @Provides get() = this

    companion object {
        private val HEARTBEAT_TIMEOUT = 45.seconds
    }
}
