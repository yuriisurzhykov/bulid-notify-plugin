package me.yuriisoft.buildnotify.network.server

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.*
import me.yuriisoft.buildnotify.serialization.HeartbeatPayload
import me.yuriisoft.buildnotify.serialization.MessageSerializer
import me.yuriisoft.buildnotify.serialization.WsEnvelope
import me.yuriisoft.buildnotify.settings.PluginSettingsState

/**
 * SRP: only sends periodic heartbeats.
 *
 * Constructor dependencies — DIP is met.
 * Doesn't know where the registry and settings come from.
 *
 * SupervisorJob: an exception in a single tick doesn't kill the entire scheduler.
 * Lifecycle: strictly start() → stop(), not tied to the IDE application scope.
 */
class HeartbeatScheduler(
    private val registry: ClientRegistry,
    private val settingsProvider: () -> PluginSettingsState.State,
) {
    private val logger = thisLogger()

    private var scope: CoroutineScope? = null
    private var tickJob: Job? = null

    fun start() {
        if (tickJob?.isActive == true) return

        val newScope = CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO +
                    CoroutineName("BuildNotifyHeartbeatScheduler") +
                    CoroutineExceptionHandler { _, throwable ->
                        logger.warn("Unhandled heartbeat exception", throwable)
                    }
        )

        scope = newScope
        tickJob = newScope.launch {
            while (isActive) {
                val intervalMs = settingsProvider().heartbeatIntervalSec * 1_000L
                delay(intervalMs)
                tick()
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        scope?.cancel()
        scope = null
    }

    fun resetTick() {
        tickJob?.cancel()
        tickJob = null
        start()
    }

    private fun tick() {
        if (registry.hasNoOpenClients()) return

        runCatching {
            val message = MessageSerializer.encode(WsEnvelope(payload = HeartbeatPayload()))
            registry.broadcast(message)
        }.onFailure { throwable ->
            logger.warn("Failed to send heartbeat", throwable)
        }
    }
}