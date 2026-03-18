package me.yuriisoft.buildnotify.build

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import me.yuriisoft.buildnotify.build.model.BuildGraphEvent
import me.yuriisoft.buildnotify.build.model.BuildResult
import me.yuriisoft.buildnotify.serialization.WsMessage
import me.yuriisoft.buildnotify.server.BuildWebSocketServer

@Service(Service.Level.APP)
class BuildNotificationPublisher {

    fun publishStarted(projectName: String, buildId: String) {
        publish(
            WsMessage.BuildStarted(
                projectName = projectName,
                buildId = buildId,
            )
        )
    }

    fun publishGraphEvent(event: BuildGraphEvent) {
        publish(WsMessage.BuildGraphEventMessage(event))
    }

    fun publishResult(result: BuildResult) {
        publish(WsMessage.BuildResultMessage(result))
    }

    private fun publish(message: WsMessage) {
        val server = service<BuildWebSocketServer>()
        if (!server.isActive()) return
        server.broadcast(message)
    }
}