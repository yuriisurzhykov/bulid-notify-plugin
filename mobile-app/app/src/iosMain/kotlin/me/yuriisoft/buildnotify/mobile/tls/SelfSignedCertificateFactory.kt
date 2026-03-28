package me.yuriisoft.buildnotify.mobile.tls

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSMutableData
import platform.Foundation.appendBytes
import platform.Foundation.create
import platform.Security.SecCertificateCreateWithData
import platform.Security.SecCertificateRef
import platform.Security.SecKeyCreateSignature
import platform.Security.SecKeyRef
import platform.Security.kSecKeyAlgorithmRSASignatureDigestPKCS1v15SHA256

/**
 * Minimal self-signed X.509 certificate factory for the iOS mTLS client identity.
 *
 * ### Why this class exists
 * iOS Security framework (`Security.framework`) provides `SecCertificateCreateWithData`
 * to *import* DER-encoded certificates, but has no API to *generate* them. This
 * class constructs the minimal DER bytes required for a valid self-signed certificate
 * so that [ClientIdentityManager] can produce a complete `SecIdentity` without
 * depending on OpenSSL or any third-party library.
 *
 * ### What "minimal" means
 * The output cert is a v1 X.509 certificate:
 *   - Version:    v1 (omitted — v1 is the default per RFC 5280)
 *   - Serial:     1
 *   - Algorithm:  sha256WithRSAEncryption (OID 1.2.840.113549.1.1.11)
 *   - Subject/Issuer: provided [subject] string encoded as UTF8String in a RDN
 *   - Validity:   from now for [validityDays]
 *   - Public key: RSA public key from [publicKey] exported as SubjectPublicKeyInfo
 *   - Signature:  SHA256withRSA signed with [privateKey] via `SecKeyCreateSignature`
 *
 * This subset is sufficient for the server's `ClientToFuTrustManager.checkClientTrusted()`
 * which only needs to extract the SHA-256 fingerprint of the DER-encoded certificate.
 * No chain validation is performed (server returns `emptyArray()` from `getAcceptedIssuers()`).
 *
 * ### Responsibilities (SRP)
 * This class knows ASN.1/DER encoding. It has no knowledge of:
 * - Keychain storage — see [ClientIdentityManager].
 * - TLS configuration — see [DarwinHttpClientProvider].
 *
 * ### Thread-safety
 * All public methods are pure (stateless). Calls to `SecKeyCreateSignature`
 * are thread-safe per Apple documentation.
 */
@OptIn(ExperimentalForeignApi::class)
internal object SelfSignedCertificateFactory {

    /**
     * Creates a self-signed [SecCertificateRef] from the supplied RSA key pair.
     *
     * @param publicKey  the RSA public key whose SubjectPublicKeyInfo is embedded.
     * @param privateKey the RSA private key used to sign the TBSCertificate.
     * @param subject    X.500 common name value (e.g. `"CN=BuildNotify-Client"`).
     * @param validityDays certificate validity period in days.
     * @return a `SecCertificateRef` ready for Keychain insertion, or `null` on any error.
     */
    fun create(
        publicKey: SecKeyRef,
        privateKey: SecKeyRef,
        subject: String,
        validityDays: Int,
    ): SecCertificateRef? = memScoped {
        // Export the public key as SubjectPublicKeyInfo DER bytes.
        val spkiBytes = exportPublicKeyAsSPKI(publicKey) ?: return null

        // Build the TBSCertificate (the "to-be-signed" part).
        val tbsBytes = buildTbsCertificate(
            subject = subject,
            validityDays = validityDays,
            subjectPublicKeyInfo = spkiBytes,
        )

        // Sign the TBSCertificate with SHA256withRSA.
        val tbsNsData = NSData.create(bytes = tbsBytes)
        val signatureData = SecKeyCreateSignature(
            privateKey,
            kSecKeyAlgorithmRSASignatureDigestPKCS1v15SHA256,
            tbsNsData as platform.CoreFoundation.CFDataRef,
            null,
        ) ?: return null

        @Suppress("UNCHECKED_CAST")
        val sigBytes = (signatureData as NSData).toByteArray()

        // Wrap into a full Certificate DER sequence.
        val certDer = buildCertificate(tbsBytes, sigBytes)
        val certData = NSData.create(bytes = certDer)

        SecCertificateCreateWithData(
            null,
            certData as platform.CoreFoundation.CFDataRef,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TBSCertificate builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encodes the TBSCertificate (RFC 5280 §4.1.2).
     *
     * ```
     * TBSCertificate ::= SEQUENCE {
     *     serialNumber         INTEGER,
     *     signature            AlgorithmIdentifier,
     *     issuer               Name,
     *     validity             Validity,
     *     subject              Name,
     *     subjectPublicKeyInfo SubjectPublicKeyInfo
     * }
     * ```
     *
     * Version is omitted (defaults to v1). Extensions are omitted (not needed
     * for the fingerprint-only trust model used by `ClientToFuTrustManager`).
     */
    private fun buildTbsCertificate(
        subject: String,
        validityDays: Int,
        subjectPublicKeyInfo: ByteArray,
    ): ByteArray {
        val serial = derInteger(byteArrayOf(0x01))
        val signatureAlg = derSha256WithRsaAlgorithm()
        val name = derName(subject)
        val validity = derValidity(validityDays)

        val tbsContent = serial + signatureAlg + name + validity + name + subjectPublicKeyInfo
        return derSequence(tbsContent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Full Certificate builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps TBSCertificate, AlgorithmIdentifier, and BIT STRING signature into the
     * outer Certificate SEQUENCE (RFC 5280 §4.1).
     */
    private fun buildCertificate(tbs: ByteArray, signature: ByteArray): ByteArray {
        val signatureAlg = derSha256WithRsaAlgorithm()
        val sigBitString = derBitString(signature)
        return derSequence(tbs + signatureAlg + sigBitString)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SubjectPublicKeyInfo export
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exports [publicKey] as DER-encoded SubjectPublicKeyInfo.
     *
     * `SecKeyCopyExternalRepresentation` on RSA returns the raw public key in
     * PKCS#1 format (SEQUENCE { INTEGER n, INTEGER e }). We wrap it in a
     * SubjectPublicKeyInfo SEQUENCE with the RSA algorithm OID.
     */
    private fun exportPublicKeyAsSPKI(publicKey: SecKeyRef): ByteArray? {
        @Suppress("UNCHECKED_CAST")
        val keyData = platform.Security.SecKeyCopyExternalRepresentation(publicKey, null)
            ?: return null
        val pkcs1Bytes = (keyData as NSData).toByteArray()
        val algorithmIdentifier = derSha256WithRsaAlgorithmForSpki()
        val bitString = derBitString(pkcs1Bytes)
        return derSequence(algorithmIdentifier + bitString)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASN.1 / DER primitives
    // ─────────────────────────────────────────────────────────────────────────

    /** DER SEQUENCE wrapper: tag 0x30 + length + [content]. */
    private fun derSequence(content: ByteArray): ByteArray =
        byteArrayOf(0x30.toByte()) + derLength(content.size) + content

    /**
     * DER INTEGER: tag 0x02 + length + [value].
     * A leading 0x00 is prepended if the high bit of the first byte is set
     * (to ensure the integer is interpreted as positive per DER).
     */
    private fun derInteger(value: ByteArray): ByteArray {
        val padded = if (value.isNotEmpty() && value[0].toInt() and 0x80 != 0) {
            byteArrayOf(0x00) + value
        } else {
            value
        }
        return byteArrayOf(0x02.toByte()) + derLength(padded.size) + padded
    }

    /**
     * DER BIT STRING: tag 0x03 + length + 0x00 (unused bits) + [data].
     */
    private fun derBitString(data: ByteArray): ByteArray =
        byteArrayOf(0x03.toByte()) + derLength(data.size + 1) + byteArrayOf(0x00) + data

    /**
     * DER UTF8String: tag 0x0C + length + UTF-8 bytes.
     * Used for the CN= value inside the Name.
     */
    private fun derUtf8String(value: String): ByteArray {
        val bytes = value.encodeToByteArray()
        return byteArrayOf(0x0C.toByte()) + derLength(bytes.size) + bytes
    }

    /**
     * DER OID encoding.
     *
     * The first two components (a, b) are encoded as `40*a + b`.
     * Subsequent components use base-128 big-endian encoding with the
     * high bit set on all but the last byte.
     */
    private fun derOid(oid: IntArray): ByteArray {
        val result = mutableListOf<Byte>()
        result.add(0x06.toByte())  // tag

        val encoded = mutableListOf<Byte>()
        encoded.add((40 * oid[0] + oid[1]).toByte())
        for (i in 2 until oid.size) {
            val component = oid[i]
            if (component < 128) {
                encoded.add(component.toByte())
            } else {
                val buf = mutableListOf<Byte>()
                var v = component
                buf.add(0, (v and 0x7F).toByte())
                v = v shr 7
                while (v > 0) {
                    buf.add(0, ((v and 0x7F) or 0x80).toByte())
                    v = v shr 7
                }
                encoded.addAll(buf)
            }
        }
        result.addAll(derLength(encoded.size).toList())
        result.addAll(encoded)
        return result.toByteArray()
    }

    /**
     * Encodes a DER length field:
     * - < 128:  single byte
     * - >= 128: 0x80 | n_bytes, followed by n big-endian bytes
     */
    private fun derLength(length: Int): ByteArray = when {
        length < 0x80  -> byteArrayOf(length.toByte())
        length < 0x100 -> byteArrayOf(0x81.toByte(), length.toByte())
        else           -> byteArrayOf(
            0x82.toByte(),
            ((length shr 8) and 0xFF).toByte(),
            (length and 0xFF).toByte(),
        )
    }

    /**
     * AlgorithmIdentifier for `sha256WithRSAEncryption` (OID 1.2.840.113549.1.1.11)
     * with NULL parameters — used in both the TBSCertificate signature field and
     * the outer Certificate signature field.
     */
    private fun derSha256WithRsaAlgorithm(): ByteArray {
        val oid = derOid(intArrayOf(1, 2, 840, 113549, 1, 1, 11))
        val nullParam = byteArrayOf(0x05, 0x00)  // ASN.1 NULL
        return derSequence(oid + nullParam)
    }

    /**
     * AlgorithmIdentifier for `rsaEncryption` (OID 1.2.840.113549.1.1.1)
     * used inside SubjectPublicKeyInfo to identify the key algorithm.
     */
    private fun derSha256WithRsaAlgorithmForSpki(): ByteArray {
        val oid = derOid(intArrayOf(1, 2, 840, 113549, 1, 1, 1))
        val nullParam = byteArrayOf(0x05, 0x00)
        return derSequence(oid + nullParam)
    }

    /**
     * Name ::= SEQUENCE OF RelativeDistinguishedName
     *
     * Encodes a single CN= RDN using UTF8String.
     * The OID for commonName is 2.5.4.3.
     */
    private fun derName(cn: String): ByteArray {
        val cnOid = derOid(intArrayOf(2, 5, 4, 3))
        val cnValue = derUtf8String(cn.removePrefix("CN="))
        val atv = derSequence(cnOid + cnValue)          // AttributeTypeAndValue
        val rdn = byteArrayOf(0x31.toByte()) + derLength(atv.size) + atv  // SET OF atv
        return derSequence(rdn)                          // SEQUENCE OF rdn
    }

    /**
     * Validity ::= SEQUENCE { notBefore UTCTime, notAfter UTCTime }
     *
     * UTCTime format: `YYMMDDHHmmssZ`
     */
    private fun derValidity(validityDays: Int): ByteArray {
        val now = NSDate()
        val notBefore = formatUtcTime(now)
        val notAfter = formatUtcTime(NSDate.create(timeIntervalSinceNow = validityDays * 86_400.0))
        val notBeforeEnc = derUtcTime(notBefore)
        val notAfterEnc = derUtcTime(notAfter)
        return derSequence(notBeforeEnc + notAfterEnc)
    }

    /** DER UTCTime: tag 0x17 + length + ASCII bytes. */
    private fun derUtcTime(value: String): ByteArray {
        val bytes = value.encodeToByteArray()
        return byteArrayOf(0x17.toByte()) + derLength(bytes.size) + bytes
    }

    /**
     * Formats [date] as UTCTime string `YYMMDDHHmmssZ`.
     *
     * NSDate does not provide individual calendar components in Kotlin/Native
     * common-style, so we use `NSDateFormatter` with a fixed format.
     */
    private fun formatUtcTime(date: NSDate): String {
        val formatter = platform.Foundation.NSDateFormatter()
        formatter.dateFormat = "yyMMddHHmmss'Z'"
        formatter.timeZone = platform.Foundation.NSTimeZone.timeZoneWithName("UTC")
            ?: platform.Foundation.NSTimeZone.localTimeZone
        return formatter.stringFromDate(date)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NSData ↔ ByteArray bridge
    // ─────────────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val len = length.toInt()
        if (len == 0) return ByteArray(0)
        val result = ByteArray(len)
        result.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), bytes, len.toULong())
        }
        return result
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.Companion.create(bytes: ByteArray): NSData {
        if (bytes.isEmpty()) return NSData()
        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }
}