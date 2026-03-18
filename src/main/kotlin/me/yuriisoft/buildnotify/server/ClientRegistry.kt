package me.yuriisoft.buildnotify.server

import com.intellij.openapi.components.Service
import com.jetbrains.rd.util.ConcurrentHashMap

/**
 * SRP: storing and serving connected clients.
 *
 * ConcurrentHashMap — thread-safe: onOpen/onClose come from I/O threads.
 * Java websocket, broadcast can be called from a coroutine on Dispatchers.IO.
 *
 * Supports an arbitrary number of simultaneous clients —
 * Each device is registered with its own unique session.id.
 */
@Service(Service.Level.APP)
class ClientRegistry {

    private val clients = ConcurrentHashMap<String, WebSocketSession>()

    fun register(client: WebSocketSession) {
        clients[client.id] = client
    }

    fun unregister(clientId: String?) {
        if (clientId == null) return
        clients.remove(clientId)
    }

    fun broadcast(message: String) {
        clients.values
            .asSequence()
            .filter(WebSocketSession::isOpen)
            .forEach { session -> session.send(message) }
    }

    val connectedCount: Int
        get() = clients.values.count(WebSocketSession::isOpen)

    fun hasNoOpenClients(): Boolean =
        clients.values.none(WebSocketSession::isOpen)
}