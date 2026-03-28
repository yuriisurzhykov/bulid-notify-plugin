package me.yuriisoft.buildnotify.mobile.tls

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import me.yuriisoft.buildnotify.mobile.network.client.HttpClientProvider
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodClientCertificate
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.credentialWithIdentity
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyData
import platform.Security.SecIdentityRef
import platform.Security.SecTrustGetCertificateAtIndex
import platform.Security.SecTrustGetCertificateCount

/**
 * iOS [HttpClientProvider] that configures the Darwin engine with:
 * 1. Server certificate TOFU pinning via `NSURLAuthenticationMethodServerTrust`.
 * 2. Client certificate presentation via `NSURLAuthenticationMethodClientCertificate`
 *    (Phase 4 — mTLS).
 *
 * Both TLS sides are handled through `handleChallenge`, which is the only
 * integration point Darwin exposes for SSL customization in `NSURLSession`.
 *
 * ### Phase 4 design notes
 * - `SecIdentity` bundles the private key + certificate in the iOS Keychain.
 *   It is obtained from [ClientIdentityManager] which is responsible for
 *   generating and persisting the identity (SRP — this class only presents it).
 * - A `null` identity means the Keychain entry is missing or the device is
 *   running an older OS that doesn't support the API; in that case the
 *   `ClientCertificate` challenge is handled with `PerformDefaultHandling`
 *   which skips client-auth silently (server uses `wantClientAuth`, not
 *   `needClientAuth`, so it still accepts the connection).
 * - The `ServerTrust` challenge path is unchanged from Phase 1–3.
 *
 * @param clientIdentityManager the source of the [SecIdentityRef] that will
 *   be sent to the server during the mTLS handshake. Injected so this class
 *   remains a pure factory and does not own key-management logic (SRP / DIP).
 */
@OptIn(ExperimentalForeignApi::class)
class DarwinHttpClientProvider(
    private val clientIdentityManager: ClientIdentityManager,
) : HttpClientProvider {

    /**
     * Provides an [HttpClient] configured for the Darwin engine.
     *
     * When [fingerprint] is non-null the engine verifies that the server's leaf
     * certificate matches the TOFU-pinned value, **and** presents the client
     * identity for mTLS (if available).
     *
     * When [fingerprint] is null (plain `ws://`) no custom challenge handler is
     * installed and the default NSURLSession trust evaluation is used.
     */
    override fun provide(fingerprint: String?): HttpClient =
        HttpClient(Darwin) {
            engine {
                if (fingerprint != null) {
                    handleChallenge { _, _, challenge, completionHandler ->
                        handleChallenge(fingerprint, challenge, completionHandler)
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

    /**
     * Routes the challenge to the correct handler based on the authentication
     * method. This is the single entry point for all TLS challenges in a session.
     *
     * Challenge routing (ISP — each handler has one responsibility):
     *   - `ServerTrust`        → [handleServerTrustChallenge] — TOFU pin check
     *   - `ClientCertificate`  → [handleClientCertificateChallenge] — mTLS identity
     *   - anything else        → `PerformDefaultHandling` (system default)
     */
    private fun handleChallenge(
        expectedFingerprint: String,
        challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ) {
        when (challenge.protectionSpace.authenticationMethod) {
            NSURLAuthenticationMethodServerTrust       ->
                handleServerTrustChallenge(expectedFingerprint, challenge, completionHandler)

            NSURLAuthenticationMethodClientCertificate ->
                handleClientCertificateChallenge(completionHandler)

            else                                       ->
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
        }
    }

    /**
     * Verifies the server's leaf certificate against the TOFU-pinned
     * [expectedFingerprint]. Cancels the session if the fingerprint does not match.
     */
    private fun handleServerTrustChallenge(
        expectedFingerprint: String,
        challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ) {
        val serverTrust = challenge.protectionSpace.serverTrust
        if (serverTrust == null) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            return
        }

        val certCount = SecTrustGetCertificateCount(serverTrust)
        if (certCount == 0L) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            return
        }

        val leafCert = SecTrustGetCertificateAtIndex(serverTrust, 0)
        if (leafCert == null) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val certData = SecCertificateCopyData(leafCert) as? NSData
        if (certData == null) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            return
        }

        val actual = certData.sha256Fingerprint()

        if (actual.equals(expectedFingerprint, ignoreCase = true)) {
            val credential = NSURLCredential.credentialForTrust(serverTrust)
            completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
        } else {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        }
    }

    // Client-side mTLS
    /**
     * Responds to the server's `CertificateRequest` (the mutual-TLS challenge).
     *
     * Retrieves the stable [SecIdentityRef] from [clientIdentityManager] and
     * wraps it in an [NSURLCredential]. If the identity is not available (first
     * install race condition, Keychain error, unsupported OS), falls back to
     * `PerformDefaultHandling` — the server is configured with `wantClientAuth`
     * (not `needClientAuth`) so the handshake will still complete; the server-side
     * TOFU dialog will simply not appear for this client.
     *
     * The credential is built with [NSURLCredential.credentialWithIdentity]:
     *   - `identity`: the `SecIdentityRef` from the Keychain (contains private key ref).
     *   - `certificates`: empty — we are not a CA; the server trusts self-signed certs
     *     by their fingerprint, not by chain validation.
     *   - `persistence`: `.forSession` — credentials are not cached to disk (security).
     */
    private fun handleClientCertificateChallenge(
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ) {
        val identity: SecIdentityRef? = clientIdentityManager.getIdentity()

        if (identity == null) {
            // Identity not available — proceed without client cert.
            // Server uses wantClientAuth so this is acceptable.
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            return
        }

        @Suppress("UNCHECKED_CAST")
        val credential = NSURLCredential.credentialWithIdentity(
            identity = identity,
            certificates = NSArray(),
            persistence = platform.Foundation.NSURLCredentialPersistenceForSession,
        )
        completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
    }

    // SHA-256 fingerprint helper (server trust side)
    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.sha256Fingerprint(): String {
        val input = bytes ?: return ""
        val len = length.toInt()
        if (len == 0) return ""

        val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)
        hash.usePinned { pinned ->
            CC_SHA256(input, len.toUInt(), pinned.addressOf(0))
        }
        return hash.joinToString(":") { "%02X".format(it.toInt() and 0xFF) }
    }

    private companion object {
        const val PING_INTERVAL_MS = 15_000L
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val SOCKET_TIMEOUT_MS = 60_000L
    }
}
