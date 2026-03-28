package me.yuriisoft.buildnotify.mobile.tls

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import me.yuriisoft.buildnotify.mobile.tls.ClientCertificateManager.Companion.ALIAS
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Manages the client's identity certificate stored in the Android Keystore.
 *
 * On the first call to [ensureInitialized] a self-signed RSA-2048 certificate
 * is generated and persisted under the well-known alias [ALIAS].  The private
 * key **never leaves** the Keystore; only the [X509Certificate] is accessible
 * from outside this class.
 *
 * The SHA-256 [fingerprint] of that certificate becomes the stable identity
 * of this device installation and is transmitted to the server during the mTLS
 * handshake.
 *
 * ### Responsibilities (SRP)
 * - Check whether the alias already exists (idempotent initialisation).
 * - Generate a new key pair when absent.
 * - Expose [getCertificate] and [getPrivateKey] for the [ClientKeyManager].
 * - Expose [fingerprint] for advertising and display purposes.
 *
 * This class intentionally has **no** dependency on OkHttp, SSLContext or any
 * network type.  Crypto and key storage are its only concern.
 */
class ClientCertificateManager {

    /**
     * Ensures the key pair exists in the Keystore.
     *
     * Thread-safe via the `@Synchronized` annotation.  Calling this from the
     * app's `onCreate` (before any network activity) is recommended so that
     * subsequent [getCertificate] / [getPrivateKey] calls never block the TLS
     * handshake thread.
     */
    @Synchronized
    fun ensureInitialized() {
        if (aliasExists() && isKeyIncompatible()) {
            keyStore().deleteEntry(ALIAS)
        }
        if (!aliasExists()) generateKeyPair()
    }

    /**
     * Returns the self-signed [X509Certificate] stored under [ALIAS].
     *
     * @throws IllegalStateException if [ensureInitialized] has not been called.
     */
    fun getCertificate(): X509Certificate {
        val entry = keyStore().getCertificate(ALIAS)
            ?: error("Client certificate not found. Was ensureInitialized() called?")
        return entry as X509Certificate
    }

    /**
     * Returns the [PrivateKey] stored under [ALIAS].
     *
     * The key is backed by the hardware-secured Keystore; the raw key material
     * is never exposed to the JVM heap.
     *
     * @throws IllegalStateException if [ensureInitialized] has not been called.
     */
    fun getPrivateKey(): PrivateKey {
        val entry = keyStore().getKey(ALIAS, null)
            ?: error("Client private key not found. Was ensureInitialized() called?")
        Log.w("ClientKeyManager", "getPrivateKey: algo=${entry.algorithm}, format=${entry.format}")
        return entry as PrivateKey
    }

    /**
     * Returns the SHA-256 fingerprint of the client certificate encoded as
     * colon-separated uppercase hex pairs — the same format used by the server
     * (e.g. `"AB:CD:EF:12:34:..."`).
     *
     * This value is stable for the lifetime of the app installation.
     */
    fun fingerprint(): String = getCertificate().sha256Fingerprint()

    private fun aliasExists(): Boolean = keyStore().containsAlias(ALIAS)

    private fun isKeyIncompatible(): Boolean =
        runCatching {
            val key = keyStore().getKey(ALIAS, null) as? PrivateKey ?: return true
            javax.crypto.Cipher.getInstance("RSA/ECB/NoPadding").init(
                javax.crypto.Cipher.DECRYPT_MODE, key
            )
        }.isFailure

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }

    private fun generateKeyPair() {
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            // PURPOSE_DECRYPT is required in order AndroidKeyStore allows
            // Conscrypt to perform raw RSA operations without padding (upcall RSA/ECB/NoPadding).
            // during TSL CertificateVerify. Without it - INCOMPATIBLE_PADDING_MODE exception
            // occurs.
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT,
        )
            .setKeySize(RSA_KEY_SIZE)
            .setSignaturePaddings(
                KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
            )
            .setEncryptionPaddings(
                // Allow NoPadding to avoid INCOMPATIBLE_PADDING_MODE exception.
                // Because this is what Conscrypt requests through
                // CryptoUpcalls.rsaOpWithPrivateKey during TSL CertificateVerify.
                KeyProperties.ENCRYPTION_PADDING_NONE,
            )
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512,
                KeyProperties.DIGEST_SHA1,
                KeyProperties.DIGEST_NONE,
            )
            .setCertificateSubject(javax.security.auth.x500.X500Principal(CERT_SUBJECT))
            .setCertificateNotBefore(java.util.Date())
            .setCertificateNotAfter(tenYearsFromNow())
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
            .also { it.initialize(spec) }
            .generateKeyPair()
    }

    private fun tenYearsFromNow(): java.util.Date {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, VALIDITY_DAYS)
        return calendar.time
    }

    private fun X509Certificate.sha256Fingerprint(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val ALIAS = "buildnotify_client"
        const val CERT_SUBJECT = "CN=BuildNotify-Client"
        const val RSA_KEY_SIZE = 2048
        const val VALIDITY_DAYS = 3650 // 10 years
    }
}