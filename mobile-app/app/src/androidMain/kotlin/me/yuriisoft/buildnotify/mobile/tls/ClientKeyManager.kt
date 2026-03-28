package me.yuriisoft.buildnotify.mobile.tls

import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager

/**
 * Minimal [X509ExtendedKeyManager] that presents the BuildNotify client
 * certificate to the server during a mutual-TLS handshake.
 *
 * ### Why a separate class (SRP)
 * [OkHttpClientProvider] is a factory — key management is a distinct
 * responsibility.  [ClientCertificateManager] knows crypto and storage;
 * [ClientKeyManager] knows the JSSE API.  They compose, not merge.
 *
 * ### Why [X509ExtendedKeyManager] instead of [javax.net.ssl.X509KeyManager]
 * OkHttp's `SSLSocketFactory` wraps the engine and delegates to the extended
 * variant; using the base interface would compile but silently bypass the key
 * selection logic in some JVM versions.
 *
 * @param clientCertManager the source of truth for the certificate and key.
 */
class ClientKeyManager(
    private val clientCertManager: ClientCertificateManager,
) : X509ExtendedKeyManager() {

    // -------------------------------------------------------------------------
    // X509KeyManager — client-side selection
    // -------------------------------------------------------------------------

    override fun chooseClientAlias(
        keyType: Array<out String>,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String = ALIAS

    override fun chooseEngineClientAlias(
        keyType: Array<out String>,
        issuers: Array<out Principal>?,
        engine: SSLEngine?,
    ): String = ALIAS

    override fun getCertificateChain(alias: String?): Array<X509Certificate> =
        if (alias == ALIAS) arrayOf(clientCertManager.getCertificate()) else emptyArray()

    override fun getPrivateKey(alias: String?): PrivateKey? =
        if (alias == ALIAS) clientCertManager.getPrivateKey() else null

    // -------------------------------------------------------------------------
    // X509KeyManager — server-side selection (not used on the client)
    // -------------------------------------------------------------------------

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?,
    ): String? = null

    override fun getServerAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String> = emptyArray()

    override fun getClientAliases(
        keyType: String?,
        issuers: Array<out Principal>?,
    ): Array<String> = arrayOf(ALIAS)

    // -------------------------------------------------------------------------
    // Constant
    // -------------------------------------------------------------------------

    private companion object {
        const val ALIAS = "buildnotify_client"
    }
}