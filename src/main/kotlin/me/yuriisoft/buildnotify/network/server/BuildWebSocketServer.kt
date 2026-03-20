package me.yuriisoft.buildnotify.network.server

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import me.yuriisoft.buildnotify.serialization.*
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.DefaultSSLWebSocketServerFactory
import org.java_websocket.server.WebSocketServer
import java.io.InputStream
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Service(Service.Level.APP)
class BuildWebSocketServer : Disposable {

    private val logger = thisLogger()
    private val running = AtomicBoolean(false)

    /**
     * Stable for the lifetime of this IDE process.
     * Changes after IDE restart, which is acceptable — clients re-pair automatically.
     */
    @OptIn(ExperimentalUuidApi::class)
    private val instanceId: String = Uuid.random().toString()

    private var server: InternalWebSocketServer? = null
    private var heartbeatScheduler: HeartbeatScheduler? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return

        runCatching {
            val settings = service<PluginSettingsState>().snapshot()
            val registry = service<ClientRegistry>()

            server = InternalWebSocketServer(
                port = settings.port,
                registry = registry,
                settings = settings,
            ).apply {
                isReuseAddr = true
                connectionLostTimeout = settings.connectionLostTimeoutSec
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

    /**
     * Broadcast a payload to all connected clients.
     * The server wraps it in a fresh WsEnvelope automatically.
     */
    fun broadcast(payload: WsPayload) {
        if (!isActive()) return
        val encoded = MessageSerializer.encode(WsEnvelope(payload = payload))
        service<ClientRegistry>().broadcast(encoded)
    }

    /**
     * Send a typed response to a specific command from any client.
     * Sets correlationId = command.id so the client can match request ↔ response.
     * Currently broadcasts to all clients; target-specific send can be added via ClientRegistry later.
     */
    fun replyTo(command: WsEnvelope, payload: WsPayload) {
        if (!isActive()) return
        val encoded = MessageSerializer.encode(WsEnvelope(correlationId = command.id, payload = payload))
        service<ClientRegistry>().broadcast(encoded)
    }

    fun stop() {
        heartbeatScheduler?.stop()
        heartbeatScheduler = null

        runCatching { server?.stop(1_000) }
            .onFailure { throwable -> logger.warn("Failed to stop WebSocket server cleanly", throwable) }

        server = null
        running.set(false)

        logger.info("WebSocket server stopped")
    }

    override fun dispose() {
        stop()
    }

    private inner class InternalWebSocketServer(
        port: Int,
        private val registry: ClientRegistry,
        private val settings: PluginSettingsState.State,
    ) : WebSocketServer(InetSocketAddress(port)) {

        init {
            setupSSL(port)
        }

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            val session = WebSocketSession(socket = conn)
            conn.setAttachment(session.id)
            registry.register(session)

            val greeting = WsEnvelope(
                payload = HandshakePayload(
                    instanceId = instanceId,
                    capabilities = setOf(
                        Capability.BUILD_MONITOR,
                        Capability.BUILD_CONTROL
                    ),
                ),
            )
            session.send(MessageSerializer.encode(greeting))

            logger.info("Client connected: ${conn.remoteSocketAddress}, total=${registry.connectedCount}")
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
            registry.unregister(conn.getAttachment())
            logger.info(
                "Client disconnected: ${conn.remoteSocketAddress}, reason=$reason, remote=$remote, remaining=${registry.connectedCount}",
            )
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            val raw = message.orEmpty()
            runCatching {
                val envelope = MessageSerializer.decode(raw)
                handleClientMessage(envelope)
            }.onFailure { throwable ->
                logger.warn("Failed to decode incoming message: '$raw'", throwable)
            }
        }

        /**
         * Dispatch point for all client-initiated messages.
         * Add new command handlers here as a new `is XxxCommand` branch — nothing else changes.
         */
        private fun handleClientMessage(envelope: WsEnvelope) {
            when (val payload = envelope.payload) {
                is CancelBuildCommand -> {
                    // TODO: delegate to BuildMonitorService.cancelBuild(payload.buildId)
                    replyTo(envelope, CommandResultPayload(CommandStatus.ACCEPTED))
                    logger.debug("CancelBuild requested for buildId=${payload.buildId}")
                }

                is RunBuildCommand -> {
                    // TODO: delegate to GradleRunner / ExternalSystemManager
                    replyTo(envelope, CommandResultPayload(CommandStatus.ACCEPTED))
                    logger.debug("RunBuild requested: project=${payload.projectName}, tasks=${payload.tasks}")
                }

                else -> logger.warn(
                    "Received unexpected payload type from client: ${payload::class.simpleName}",
                )
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            logger.warn("WebSocket error on ${conn?.remoteSocketAddress}", ex)
        }

        override fun onStart() {
            logger.info("Internal WebSocket server is ready")
        }

        private fun setupSSL(port: Int) {
            val password = System.getenv("BUILDNOTIFY_KEYSTORE_PASSWORD")?.trim()?.toCharArray()
            if (password == null || password.isEmpty()) {
                logger.info(
                    "WSS disabled: set BUILDNOTIFY_KEYSTORE_PASSWORD to enable TLS (plain WS on port $port).",
                )
                return
            }

            val stream = openKeystoreStream() ?: run {
                logger.info(
                    "WSS disabled: no keystore file found (settings, BUILDNOTIFY_KEYSTORE_PATH, or resource). WS on port $port.",
                )
                return
            }

            stream.use { keystoreStream ->
                runCatching {
                    val keyStore = KeyStore.getInstance("JKS")
                    keyStore.load(keystoreStream, password)

                    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                    kmf.init(keyStore, password)

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(kmf.keyManagers, null, null)

                    setWebSocketFactory(DefaultSSLWebSocketServerFactory(sslContext))
                    logger.info("SSL (WSS) configured; secure connections accepted on port $port.")
                }.onFailure { e ->
                    logger.info("WSS not enabled; using WS only on port $port: ${e.message}")
                }
            }
        }

        private fun openKeystoreStream(): InputStream? {
            val configured = settings.keystorePath.trim()
            if (configured.isNotEmpty()) {
                val p = Path.of(configured)
                if (Files.isRegularFile(p)) return Files.newInputStream(p)
                logger.warn("Keystore path from settings not found: $configured")
            }
            val envPath = System.getenv("BUILDNOTIFY_KEYSTORE_PATH")?.trim().orEmpty()
            if (envPath.isNotEmpty()) {
                val p = Path.of(envPath)
                if (Files.isRegularFile(p)) return Files.newInputStream(p)
                logger.warn("BUILDNOTIFY_KEYSTORE_PATH not found: $envPath")
            }
            return javaClass.classLoader.getResourceAsStream("keystore.jks")
        }
    }
}
