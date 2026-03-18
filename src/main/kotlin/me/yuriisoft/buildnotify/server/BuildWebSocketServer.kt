package me.yuriisoft.buildnotify.server

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import me.yuriisoft.buildnotify.serialization.MessageSerializer
import me.yuriisoft.buildnotify.serialization.WsMessage
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application-level service: lives in the IDE forever.
 *
 * IntelliJ Platform 2023.2+ resolves the @Service constructor automatically.
 * ClientRegistry and PluginSettings are also @Service(APP), injected by the platform.
 *
 * HeartbeatScheduler is not a service, so we create it here.
 * But we pass in predefined dependencies, which means we don't violate DIP.
 */
@Service(Service.Level.APP)
class BuildWebSocketServer : Disposable {

    private val logger = thisLogger()
    private val running = AtomicBoolean(false)

    private var server: InternalServer? = null
    private var heartbeatScheduler: HeartbeatScheduler? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return

        runCatching {
            val settings = service<PluginSettingsState>().snapshot()
            val registry = service<ClientRegistry>()

            server = InternalServer(
                port = settings.port,
                registry = registry,
            ).apply {
                isReuseAddr = true
                start()
            }

            heartbeatScheduler = HeartbeatScheduler(
                registry = registry,
                settingsProvider = { service<PluginSettingsState>().snapshot() },
            ).also { it.start() }

            logger.info("WebSocket server started on port ${settings.port}")
        }.onFailure { throwable ->
            running.set(false)
            heartbeatScheduler?.stop()
            heartbeatScheduler = null
            server = null
            logger.error("Failed to start WebSocket server", throwable)
        }
    }

    fun isActive(): Boolean = running.get()

    fun broadcast(message: WsMessage) {
        if (!isActive()) return

        val encoded = MessageSerializer.encode(message)
        service<ClientRegistry>().broadcast(encoded)
    }

    override fun dispose() {
        heartbeatScheduler?.stop()
        heartbeatScheduler = null

        runCatching { server?.stop(1_000) }
            .onFailure { throwable -> logger.warn("Failed to stop WebSocket server cleanly", throwable) }

        server = null
        running.set(false)

        logger.info("WebSocket server stopped")
    }

    private inner class InternalServer(
        port: Int,
        private val registry: ClientRegistry,
    ) : WebSocketServer(InetSocketAddress(port)) {

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            val session = WebSocketSession(socket = conn)
            conn.setAttachment(session.id)
            registry.register(session)

            logger.info(
                "Client connected: ${conn.remoteSocketAddress}, total=${registry.connectedCount}"
            )
        }

        override fun onClose(
            conn: WebSocket,
            code: Int,
            reason: String?,
            remote: Boolean,
        ) {
            registry.unregister(conn.getAttachment())

            logger.info(
                "Client disconnected: ${conn.remoteSocketAddress}, reason=$reason, remote=$remote, remaining=${registry.connectedCount}"
            )
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            runCatching { MessageSerializer.decode(message.orEmpty()) }
                .onFailure { throwable ->
                    logger.warn("Failed to decode incoming message: '$message'", throwable)
                }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            logger.warn("WebSocket error on ${conn?.remoteSocketAddress}", ex)
        }

        override fun onStart() {
            logger.info("Internal WebSocket server is ready")
        }
    }
}