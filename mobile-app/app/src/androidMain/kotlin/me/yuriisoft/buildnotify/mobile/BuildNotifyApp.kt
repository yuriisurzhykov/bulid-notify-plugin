package me.yuriisoft.buildnotify.mobile

import android.app.Application
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.AndroidNsdDiscovery
import me.yuriisoft.buildnotify.mobile.tls.ClientCertificateManager
import me.yuriisoft.buildnotify.mobile.tls.OkHttpClientProvider
import me.yuriisoft.buildnotify.mobile.tls.SharedPrefsTrustedServers

/**
 * Application-scoped entry point that hosts the DI [component].
 *
 * The component is created once and shared across:
 *   - [MainActivity] — for Compose UI (screens set)
 *   - [BuildMonitorService][me.yuriisoft.buildnotify.mobile.service.BuildMonitorService] — for [ConnectionManager] observation
 *
 * Declared in `AndroidManifest.xml` via `android:name`.
 *
 * ### Phase 1 change
 * [ClientCertificateManager.ensureInitialized] is called before the DI graph is
 * assembled.  This guarantees that the client certificate exists in the Android
 * Keystore before any TLS handshake can occur, without blocking the main thread
 * at connection time.
 */
class BuildNotifyApp : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()

        // Phase 1 — ensure the client identity certificate exists before any
        // network activity.  Key generation is a one-time operation that takes
        // a few hundred milliseconds; subsequent calls are effectively free.
        val clientCertManager = ClientCertificateManager().also {
            it.ensureInitialized()
        }

        component = AppComponent::class.create(
            AndroidNsdDiscovery(applicationContext),
            AndroidNetworkMonitor(applicationContext),
            AndroidAppVersionProvider(),
            SharedPrefsTrustedServers(applicationContext),
            OkHttpClientProvider(clientCertManager),
        )
    }
}