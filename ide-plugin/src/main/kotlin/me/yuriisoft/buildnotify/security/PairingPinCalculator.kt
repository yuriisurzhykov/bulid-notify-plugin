package me.yuriisoft.buildnotify.security

import java.security.MessageDigest

/**
 * Derives a 6-digit numeric PIN from the combination of server and client
 * certificate fingerprints.
 *
 * Both the IDE plugin and the mobile app independently compute the same PIN
 * using `SHA-256(serverFP.lowercase() + clientFP.lowercase()) mod 1 000 000`,
 * allowing the user to visually verify that both devices see the same value
 * without transmitting the PIN over the wire.
 *
 * Algorithm must stay identical to the mobile-side
 * `me.yuriisoft.buildnotify.mobile.network.pairing.PinCalculator`.
 */
object PairingPinCalculator {

    private const val PIN_LENGTH = 6
    private const val PIN_MODULUS = 1_000_000L

    /**
     * @param serverFingerprint SHA-256 fingerprint of the server certificate
     *                          (any hex format — colon-separated or raw).
     * @param clientFingerprint SHA-256 fingerprint of the client certificate.
     * @return A zero-padded 6-digit decimal string (e.g. `"042817"`).
     */
    fun derivePin(serverFingerprint: String, clientFingerprint: String): String {
        val combined = serverFingerprint.lowercase() + clientFingerprint.lowercase()
        val hash = MessageDigest.getInstance("SHA-256").digest(combined.encodeToByteArray())
        val numeric = hash.toBigEndianUnsignedMod(PIN_MODULUS)
        return numeric.toString().padStart(PIN_LENGTH, '0')
    }

    /**
     * Interprets this byte array as a big-endian unsigned integer and returns
     * `value mod [modulus]`.
     *
     * Uses iterative modular arithmetic identical to the KMP common-code
     * implementation so that results are guaranteed to match across platforms.
     */
    private fun ByteArray.toBigEndianUnsignedMod(modulus: Long): Long =
        fold(0L) { acc, byte ->
            (acc * 256 + (byte.toInt() and 0xFF)) % modulus
        }
}
