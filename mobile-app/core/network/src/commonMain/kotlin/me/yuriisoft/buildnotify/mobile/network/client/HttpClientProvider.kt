package me.yuriisoft.buildnotify.mobile.network.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

/**
 * Factory for a properly configured [HttpClient] used by the network layer.
 *
 * Centralises client configuration so that transport and connection classes
 * receive a ready-to-use client without knowing the setup details.
 *
 * The engine is auto-discovered at runtime from the platform classpath
 * (OkHttp on Android, Darwin on iOS).
 */
class HttpClientProvider {

    fun provide(): HttpClient = HttpClient {
        install(WebSockets) {
            pingIntervalMillis = PING_INTERVAL_MS
        }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }

    private companion object {
        const val PING_INTERVAL_MS = 15_000L
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val SOCKET_TIMEOUT_MS = 60_000L
    }
}
