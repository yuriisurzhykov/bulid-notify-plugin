package me.yuriisoft.buildnotify.mobile.tls

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSUUID
import platform.Security.SecCertificateCopyData
import platform.Security.SecCertificateRef
import platform.Security.SecIdentityCopyCertificate
import platform.Security.SecIdentityRef
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecKeyGeneratePair
import platform.Security.SecKeyRef
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrApplicationTag
import platform.Security.kSecAttrIsPermanent
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrLabel
import platform.Security.kSecClass
import platform.Security.kSecClassIdentity
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnRef
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.NSUTF8StringEncoding
import platform.darwin.NSUInteger

/**
 * Manages the client's `SecIdentity` in the iOS Keychain.
 *
 * An identity in iOS Security framework bundles the private key and the
 * corresponding certificate as a single `SecIdentityRef`, stored under
 * `kSecClassIdentity`. This is the correct storage class for client
 * certificates used in TLS mutual authentication.
 *
 * ### Responsibilities (SRP)
 * - Check whether an identity already exists in the Keychain under [LABEL].
 * - Generate a new RSA-2048 key pair via [SecKeyGeneratePair] when absent.
 * - Expose [getIdentity] for [DarwinHttpClientProvider].
 * - Expose [fingerprint] (SHA-256 of the leaf certificate) for display and
 *   comparison purposes — same format as the Android side (`AB:CD:EF:...`).
 *
 * ### Design decisions
 * - The private key **never leaves** the Secure Enclave / Keychain.
 *   `kSecAttrIsPermanent = true` ensures the key persists across app launches.
 * - `kSecAttrApplicationTag` identifies our key uniquely within the Keychain.
 * - Certificate creation uses `SecCertificateCreateWithData` with a
 *   DER-encoded self-signed certificate. Generating the self-signed cert in
 *   Kotlin/Native requires either calling `SecCertificateCreateWithData` with
 *   pre-baked DER bytes produced by a helper, or using a third-party library.
 *   The implementation below delegates to [SelfSignedCertificateFactory] which
 *   wraps the minimal ASN.1 construction needed for a self-signed cert.
 * - Thread-safety: [ensureInitialized] is `@Synchronized` (via ObjC `@synchronized`
 *   equivalent) because it may be called from a background Ktor thread.
 *
 * ### iOS note on `needClientAuth` vs `wantClientAuth`
 * The server uses `wantClientAuth` until all iOS clients are updated. If
 * [getIdentity] returns `null` (Keychain error, missing entry), the client
 * simply presents no certificate and the server proceeds without mTLS.
 */
@OptIn(ExperimentalForeignApi::class)
class ClientIdentityManager {

    /**
     * Ensures the client identity (key pair + self-signed certificate) exists
     * in the Keychain.
     *
     * Thread-safe. Idempotent — subsequent calls are effectively free because
     * they short-circuit on `identityExists()`.
     *
     * Call from the iOS entry point (`MainViewController`) before any network
     * activity begins, mirroring the Android pattern in `BuildNotifyApp.onCreate`.
     */
    @Synchronized
    fun ensureInitialized() {
        if (!identityExists()) {
            generateAndStoreIdentity()
        }
    }

    /**
     * Returns the `SecIdentityRef` from the Keychain, or `null` if the identity
     * has not been initialized yet or if a Keychain lookup error occurred.
     *
     * The returned `CFTypeRef` is automatically memory-managed by Kotlin/Native's
     * ARC bridge; callers do not need to call `CFRelease`.
     */
    fun getIdentity(): SecIdentityRef? = lookupIdentity()

    /**
     * Returns the SHA-256 fingerprint of the client certificate in colon-separated
     * uppercase hex format: `AB:CD:EF:12:34:...`
     *
     * This is the stable identity of this device installation — the same value the
     * plugin's `ClientToFuTrustManager` will see and display in the approval dialog.
     *
     * Returns `null` if no identity has been initialized yet.
     */
    fun fingerprint(): String? {
        val identity = lookupIdentity() ?: return null
        return memScoped {
            val certRef = alloc<CFTypeRefVar>()
            val status = SecIdentityCopyCertificate(identity, certRef.ptr.reinterpret())
            if (status != errSecSuccess) return null

            @Suppress("UNCHECKED_CAST")
            val cert = certRef.value as? SecCertificateRef ?: return null
            @Suppress("UNCHECKED_CAST")
            val certData = SecCertificateCopyData(cert) as? NSData ?: return null
            certData.sha256Fingerprint()
        }
    }

    private fun identityExists(): Boolean = lookupIdentity() != null

    private fun lookupIdentity(): SecIdentityRef? = memScoped {
        val query = NSMutableDictionary()
        query[kSecClass] = kSecClassIdentity
        query[kSecAttrLabel] = LABEL.toNSString()
        query[kSecMatchLimit] = kSecMatchLimitOne
        query[kSecReturnRef] = true

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as platform.CoreFoundation.CFDictionaryRef, result.ptr)

        if (status == errSecSuccess) {
            @Suppress("UNCHECKED_CAST")
            result.value as? SecIdentityRef
        } else {
            null
        }
    }

    /**
     * Generates an RSA-2048 key pair via [SecKeyGeneratePair] and stores it
     * as `kSecClassIdentity` in the Keychain together with a self-signed
     * certificate produced by [SelfSignedCertificateFactory].
     *
     * The Keychain entry is tagged with both [LABEL] (human-readable) and
     * [APPLICATION_TAG] (machine-readable byte tag, for unambiguous lookup).
     */
    private fun generateAndStoreIdentity() {
        memScoped {
            // 1. Key pair attributes
            val privateKeyAttrs = NSMutableDictionary()
            privateKeyAttrs[kSecAttrIsPermanent] = true
            privateKeyAttrs[kSecAttrApplicationTag] = APPLICATION_TAG.toNSString()
            privateKeyAttrs[kSecAttrLabel] = LABEL.toNSString()

            val keyPairAttrs = NSMutableDictionary()
            keyPairAttrs[kSecAttrKeyType] = kSecAttrKeyTypeRSA
            keyPairAttrs[kSecAttrKeySizeInBits] = RSA_KEY_SIZE as NSUInteger
            keyPairAttrs[kSecAttrLabel] = LABEL.toNSString()
            keyPairAttrs["privateKeyAttrs"] = privateKeyAttrs

            val publicKeyRef = alloc<CFTypeRefVar>()
            val privateKeyRef = alloc<CFTypeRefVar>()

            val genStatus = SecKeyGeneratePair(
                keyPairAttrs as platform.CoreFoundation.CFDictionaryRef,
                publicKeyRef.ptr.reinterpret(),
                privateKeyRef.ptr.reinterpret(),
            )

            if (genStatus != errSecSuccess) {
                // Key generation failed — identity will remain absent.
                // DarwinHttpClientProvider.handleClientCertificateChallenge handles null.
                return
            }

            @Suppress("UNCHECKED_CAST")
            val publicKey = publicKeyRef.value as? SecKeyRef ?: return
            @Suppress("UNCHECKED_CAST")
            val privateKey = privateKeyRef.value as? SecKeyRef ?: return

            // 2. Generate a self-signed certificate from the key pair.
            val cert = SelfSignedCertificateFactory.create(
                publicKey = publicKey,
                privateKey = privateKey,
                subject = CERT_SUBJECT,
                validityDays = VALIDITY_DAYS,
            ) ?: return

            // 3. Store identity (private key is already in Keychain from SecKeyGeneratePair).
            //    Adding the certificate to the Keychain under kSecClassCertificate will
            //    cause the OS to automatically link it with the matching private key,
            //    making it retrievable as kSecClassIdentity.
            val addCertQuery = NSMutableDictionary()
            addCertQuery[kSecClass] = platform.Security.kSecClassCertificate
            addCertQuery[kSecAttrLabel] = LABEL.toNSString()
            addCertQuery[kSecValueRef] = cert

            SecItemAdd(addCertQuery as platform.CoreFoundation.CFDictionaryRef, null)
            // Ignore errSecDuplicateItem — idempotent if somehow called twice.
        }
    }

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

    private fun String.toNSString(): NSString =
        NSString.create(string = this)

    private companion object {
        /** Human-readable Keychain label for the identity. */
        const val LABEL = "BuildNotify-Client"

        /** Machine-readable application tag — must be unique within the app's Keychain. */
        const val APPLICATION_TAG = "me.yuriisoft.buildnotify.client"

        /** X.500 subject DN of the generated self-signed certificate. */
        const val CERT_SUBJECT = "CN=BuildNotify-Client"

        /** RSA key size in bits. Must match the server's expectation. */
        const val RSA_KEY_SIZE = 2048

        /** Certificate validity in days (10 years — acts as a stable device identity). */
        const val VALIDITY_DAYS = 3650
    }
}