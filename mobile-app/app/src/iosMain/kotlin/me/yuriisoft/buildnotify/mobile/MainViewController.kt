package me.yuriisoft.buildnotify.mobile

import androidx.compose.ui.window.ComposeUIViewController
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.IosNsdDiscovery
import me.yuriisoft.buildnotify.mobile.tls.ClientIdentityManager
import me.yuriisoft.buildnotify.mobile.tls.DarwinHttpClientProvider
import me.yuriisoft.buildnotify.mobile.tls.UserDefaultsTrustedServers
import platform.UIKit.UIViewController

/**
 * iOS entry point for the Compose Multiplatform UI.
 *
 * ### Phase 4 change
 * [ClientIdentityManager.ensureInitialized] is called before the DI graph is
 * assembled — mirroring the Android pattern in `BuildNotifyApp.onCreate`.
 * This guarantees the client identity (RSA key pair + self-signed certificate)
 * exists in the Keychain before any TLS handshake can occur.
 *
 * Key generation is a one-time operation (a few hundred milliseconds on first
 * install). Subsequent launches call [ClientIdentityManager.getIdentity] which
 * is a fast Keychain lookup.
 *
 * [ClientIdentityManager] is passed to [DarwinHttpClientProvider] so that the
 * Darwin engine can present the client certificate during the mutual-TLS
 * `NSURLAuthenticationMethodClientCertificate` challenge.
 */
fun MainViewController(): UIViewController {
    val clientIdentityManager = ClientIdentityManager().also {
        it.ensureInitialized()
    }

    val component = AppComponent::class.create(
        IosNsdDiscovery(),
        IosNetworkMonitor(),
        IosAppVersionProvider(),
        UserDefaultsTrustedServers(),
        DarwinHttpClientProvider(clientIdentityManager),
    )

    return ComposeUIViewController {
        App(
            screens = component.screens,
            startRoute = component.startRoute,
        )
    }
}
