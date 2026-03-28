package me.yuriisoft.buildnotify.mobile

import com.yuriisurzhykov.buildnotifier.feature.catalog.CatalogComponent
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.core.navigation.StartRoute
import me.yuriisoft.buildnotify.mobile.core.platform.AppVersionProvider
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor
import me.yuriisoft.buildnotify.mobile.feature.buildstatus.di.BuildStatusComponent
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.INsdDiscovery
import me.yuriisoft.buildnotify.mobile.feature.discovery.di.DiscoveryComponent
import me.yuriisoft.buildnotify.mobile.navigation.ConnectionAwareStartRoute
import me.yuriisoft.buildnotify.mobile.network.NetworkComponent
import me.yuriisoft.buildnotify.mobile.network.client.HttpClientProvider
import me.yuriisoft.buildnotify.mobile.network.connection.ActiveSession
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ManagedConnection
import me.yuriisoft.buildnotify.mobile.network.error.ErrorMapping
import me.yuriisoft.buildnotify.mobile.network.reconnection.ReconnectionStrategy
import me.yuriisoft.buildnotify.mobile.network.tls.TrustedServers
import me.yuriisoft.buildnotify.mobile.network.transport.Transport
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
    @get:Provides protected val trustedServers: TrustedServers,
    @get:Provides protected val httpClientProvider: HttpClientProvider,
) : DiscoveryComponent,
    NetworkComponent,
    BuildStatusComponent,
    CatalogComponent {

    abstract val screens: Set<Screen>
    abstract val connectionManager: ConnectionManager
    abstract val startRoute: StartRoute

    // --- App-level bindings only ---

    @Provides
    protected fun dispatchers(): AppDispatchers = AppDispatchers.Default()

    @Provides
    protected fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    protected fun startRoute(
        connectionManager: ConnectionManager,
    ): StartRoute = ConnectionAwareStartRoute(connectionManager)

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