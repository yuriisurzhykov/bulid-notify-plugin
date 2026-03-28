package me.yuriisoft.buildnotify.mobile.network.connection

/**
 * A Build Notify plugin instance resolved to a connectable endpoint.
 *
 * [name] is the human-readable service name (e.g. "MyMacBook IDE").
 * [host] is the resolved IP address or hostname.
 * [port] is the WebSocket server port.
 * [scheme] is `"wss"` when the server advertises TLS, `"ws"` otherwise.
 * [fingerprint] is the SHA-256 certificate fingerprint from the mDNS TXT
 * record (`fp` key), used for TOFU pin verification. `null` when TLS is
 * not advertised.
 *
 * [instanceId] is parsed from the mDNS TXT record `id` key.
 * Why this field exists: `name` (the mDNS service name) is user-configurable
 * and changes if the user renames their machine. Using it as a [TrustedServers]
 * key causes spurious re-pairing. [instanceId] is immutable for the lifetime
 * of an IDE process and is the correct key.
 *
 * `null` when connecting to an older plugin version that doesn't yet advertise
 * the `id` TXT field — callers fall back gracefully to `name` in that case.
 *
 * Lives in `:core:network` so that both the connection layer and feature
 * modules share one definition without circular dependencies.
 */
data class DiscoveredHost(
    val name: String,
    val host: String,
    val port: Int,
    val scheme: String = "ws",
    val fingerprint: String? = null,
    val instanceId: String? = null,
) {
    val trustKey: String get() = instanceId ?: name
    val isSecure: Boolean get() = scheme == "wss"
}
