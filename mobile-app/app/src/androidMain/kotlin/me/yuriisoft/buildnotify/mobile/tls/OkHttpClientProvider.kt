package me.yuriisoft.buildnotify.mobile.tls

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import me.yuriisoft.buildnotify.mobile.network.client.HttpClientProvider
import me.yuriisoft.buildnotify.mobile.tls.OkHttpClientProvider.Companion.PLAIN_KEY
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Android [HttpClientProvider] that configures OkHttp with:
 * 1. Server certificate pinning via [BuildNotifyTrustManager] (TOFU — existing).
 * 2. Client certificate presentation via [ClientKeyManager] (mTLS — Phase 1).
 *
 * Both concerns are active only when a non-null [fingerprint] is supplied,
 * i.e. the connection is to a known, paired server.  Plain `ws://` connections
 * (fingerprint == null) remain unchanged.
 *
 * ### Cache key
 * Previously the key was `fingerprint ?: PLAIN_KEY`.  With mTLS the semantically
 * correct key is `"$serverFingerprint|$clientFingerprint"` — even though the app
 * currently has exactly one client certificate, the composite key ensures two
 * hypothetical clients with the same server fingerprint but different client
 * identities would never share a cache entry.
 *
 * @param clientCertManager source of the client certificate.  Injected so that
 *   [OkHttpClientProvider] remains a pure factory (SRP) and can be unit-tested
 *   with a fake [ClientCertificateManager].
 */
class OkHttpClientProvider(
    private val clientCertManager: ClientCertificateManager,
) : HttpClientProvider {

    private val cache = mutableMapOf<String, HttpClient>()

    override fun provide(fingerprint: String?): HttpClient {
        val key = cacheKey(fingerprint)
        return cache.getOrPut(key) { buildClient(fingerprint) }
    }

    override fun release(fingerprint: String?) {
        val key = cacheKey(fingerprint)
        cache.remove(key)?.close()
    }

    // Private — client construction
    private fun buildClient(fingerprint: String?): HttpClient {
        val trustManager: X509TrustManager? = fingerprint?.let {
            BuildNotifyTrustManager(it)
        }

        return HttpClient(OkHttp) {
            engine {
                config {
                    if (trustManager != null) {
                        val keyManagers = arrayOf(ClientKeyManager(clientCertManager))
                        val trustManagers = arrayOf(trustManager)

                        val sslContext = SSLContext.getInstance("TLS").apply {
                            init(keyManagers, trustManagers, SecureRandom())
                        }
                        sslSocketFactory(sslContext.socketFactory, trustManager)
                        hostnameVerifier { _, _ -> true }
                    }
                }
            }
            install(WebSockets) {
                pingIntervalMillis = PING_INTERVAL_MS
            }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
        }
    }

    // Private — cache key
    /**
     * Composite cache key: `"$serverFingerprint|$clientFingerprint"` for secure
     * connections, or [PLAIN_KEY] for plain connections.
     *
     * The client fingerprint is included so that the key remains semantically
     * correct even if multiple client identities exist in the future.
     */
    private fun cacheKey(serverFingerprint: String?): String {
        if (serverFingerprint == null) return PLAIN_KEY
        val clientFingerprint = runCatching { clientCertManager.fingerprint() }
            .getOrDefault(UNKNOWN_CLIENT)
        return "$serverFingerprint|$clientFingerprint"
    }

    // Constants
    private companion object {
        const val PLAIN_KEY = "__plain__"
        const val UNKNOWN_CLIENT = "__unknown_client__"
        const val PING_INTERVAL_MS = 15_000L
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val SOCKET_TIMEOUT_MS = 60_000L
    }
}