package me.yuriisoft.buildnotify.mobile.domain.model

/**
 * A Build Notify plugin instance discovered via mDNS/NSD.
 *
 * [name] is the human-readable service name (e.g. "MyMacBook IDE").
 * [host] is the resolved IP address or hostname.
 * [port] is the WebSocket server port.
 */
data class DiscoveredHost(
    val name: String,
    val host: String,
    val port: Int,
)
