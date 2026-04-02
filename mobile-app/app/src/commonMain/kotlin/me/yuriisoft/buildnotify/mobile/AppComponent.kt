package me.yuriisoft.buildnotify.mobile

import com.yuriisurzhykov.buildnotifier.feature.catalog.CatalogComponent
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.cache.db.CacheDatabase
import me.yuriisoft.buildnotify.mobile.core.cache.db.CacheDatabaseFactory
import me.yuriisoft.buildnotify.mobile.core.cache.db.CacheEntryQueries
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.core.navigation.StartRoute
import me.yuriisoft.buildnotify.mobile.core.platform.AppVersionProvider
import me.yuriisoft.buildnotify.mobile.core.platform.DeviceIdentity
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor
import me.yuriisoft.buildnotify.mobile.crypto.platformSha256
import me.yuriisoft.buildnotify.mobile.data.protocol.HelloPayload
import me.yuriisoft.buildnotify.mobile.feature.buildstatus.di.BuildStatusComponent
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.INsdDiscovery
import me.yuriisoft.buildnotify.mobile.feature.discovery.di.DiscoveryComponent
import me.yuriisoft.buildnotify.mobile.feature.networkstatus.di.NetworkStatusComponent
import me.yuriisoft.buildnotify.mobile.navigation.ConnectionAwareStartRoute
import me.yuriisoft.buildnotify.mobile.network.NetworkComponent
import me.yuriisoft.buildnotify.mobile.network.client.HttpClientProvider
import me.yuriisoft.buildnotify.mobile.network.connection.ActiveSession
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionOrchestrator
import me.yuriisoft.buildnotify.mobile.network.connection.SecureSession
import me.yuriisoft.buildnotify.mobile.network.error.ErrorMapping
import me.yuriisoft.buildnotify.mobile.network.pairing.PairingCoordinator
import me.yuriisoft.buildnotify.mobile.network.pairing.PinCalculator
import me.yuriisoft.buildnotify.mobile.network.reconnection.ReconnectionStrategy
import me.yuriisoft.buildnotify.mobile.network.sync.BuildStateSync
import me.yuriisoft.buildnotify.mobile.network.tls.ClientIdentityProvider
import me.yuriisoft.buildnotify.mobile.network.tls.ServerCertificateCapture
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
    @get:Provides protected val deviceIdentity: DeviceIdentity,
    @get:Provides protected val trustedServers: TrustedServers,
    @get:Provides protected val httpClientProvider: HttpClientProvider,
    @get:Provides protected val serverCertCapture: ServerCertificateCapture,
    @get:Provides protected val clientIdentityProvider: ClientIdentityProvider,
    @get:Provides protected val cacheDatabaseFactory: CacheDatabaseFactory,
) : DiscoveryComponent,
    NetworkComponent,
    BuildStatusComponent,
    CatalogComponent,
    NetworkStatusComponent {

    abstract val screens: Set<Screen>
    abstract val connectionManager: ConnectionManager
    abstract val startRoute: StartRoute

    // --- App-level bindings only ---

    @Provides
    protected fun dispatchers(): AppDispatchers = AppDispatchers.Default()

    @Provides
    protected fun json(): Json = Json { ignoreUnknownKeys = true }

    @AppScope
    @Provides
    protected fun cacheDatabase(factory: CacheDatabaseFactory): CacheDatabase =
        factory.create()

    @Provides
    protected fun cacheEntryQueries(db: CacheDatabase): CacheEntryQueries =
        db.cacheEntryQueries

    @Provides
    protected fun startRoute(
        connectionManager: ConnectionManager,
    ): StartRoute = ConnectionAwareStartRoute(connectionManager)

    @Provides
    protected fun pinCalculator(): PinCalculator =
        PinCalculator(::platformSha256)

    @AppScope
    @Provides
    protected fun pairingCoordinator(
        pinCalculator: PinCalculator,
        trustedServers: TrustedServers,
        clientIdentity: ClientIdentityProvider,
    ): PairingCoordinator = PairingCoordinator(pinCalculator, trustedServers, clientIdentity)

    @AppScope
    @Provides
    protected fun secureSession(): SecureSession = SecureSession()

    @AppScope
    @Provides
    protected fun connectionOrchestrator(
        transport: Transport,
        reconnection: ReconnectionStrategy,
        errorMapping: ErrorMapping,
        pairingCoordinator: PairingCoordinator,
        session: SecureSession,
        serverCertCapture: ServerCertificateCapture,
        deviceIdentity: DeviceIdentity,
        appVersionProvider: AppVersionProvider,
        dispatchers: AppDispatchers,
    ): ConnectionOrchestrator = ConnectionOrchestrator(
        transport = transport,
        reconnection = reconnection,
        errorMapping = errorMapping,
        pairingCoordinator = pairingCoordinator,
        session = session,
        serverCertCapture = serverCertCapture,
        helloPayload = HelloPayload(
            deviceName = deviceIdentity.deviceName,
            platform = deviceIdentity.platform,
            appVersion = appVersionProvider.versionName,
        ),
        heartbeatTimeout = HEARTBEAT_TIMEOUT,
        dispatchers = dispatchers,
    )

    @AppScope
    @Provides
    protected fun buildStateSync(
        session: SecureSession,
        dispatchers: AppDispatchers,
    ): BuildStateSync = BuildStateSync(session, dispatchers)

    protected val SecureSession.bindSession: ActiveSession
        @Provides get() = this

    protected val ConnectionOrchestrator.bindManager: ConnectionManager
        @Provides get() = this

    companion object {
        private val HEARTBEAT_TIMEOUT = 45.seconds
    }
}
