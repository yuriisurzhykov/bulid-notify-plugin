package me.yuriisoft.buildnotify.network.discovery

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

@Service(Service.Level.APP)
class MdnsAdvertiser : Disposable {

    companion object {
        const val SERVICE_TYPE = "_buildnotify._tcp.local."
    }

    private val logger = thisLogger()
    private val started = AtomicBoolean(false)
    private val jmDNS: AtomicReference<JmDNS?> = AtomicReference(null)

    fun start() {
        if (!started.compareAndSet(false, true)) return

        runCatching {
            val settings = service<PluginSettingsState>().snapshot()

            val mDnsInstance = JmDNS.create(InetAddress.getLocalHost())
            val info = ServiceInfo.create(
                SERVICE_TYPE,
                settings.serviceName,
                settings.port,
                0,
                0,
                mapOf("version" to "1"),
            )

            mDnsInstance.registerService(info)
            jmDNS.getAndSet(mDnsInstance)
            logger.info("mDNS advertiser started: ${settings.serviceName}:${settings.port}")
        }.onFailure { throwable ->
            started.set(false)
            logger.error("Failed to start mDNS advertiser", throwable)
        }
    }

    fun stop() {
        runCatching { jmDNS.getAndSet(null)?.close() }
            .onFailure { throwable -> logger.warn("Failed to stop mDNS advertiser cleanly", throwable) }
        started.set(false)
    }

    override fun dispose() {
        stop()
    }
}