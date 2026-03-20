package me.yuriisoft.buildnotify.build

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import me.yuriisoft.buildnotify.build.model.BuildResult
import me.yuriisoft.buildnotify.build.pipeline.OutgoingBuildEvent
import me.yuriisoft.buildnotify.network.server.BuildWebSocketServer
import me.yuriisoft.buildnotify.serialization.BuildDiagnosticPayload
import me.yuriisoft.buildnotify.serialization.BuildResultPayload
import me.yuriisoft.buildnotify.serialization.BuildStartedPayload
import me.yuriisoft.buildnotify.serialization.TaskFinishedPayload
import me.yuriisoft.buildnotify.serialization.TaskStartedPayload
import me.yuriisoft.buildnotify.serialization.WsPayload

/**
 * Bridges the build-event pipeline and the WebSocket transport layer.
 *
 * Converts domain-level [OutgoingBuildEvent]s into serializable [WsPayload]s
 * and broadcasts them to all connected clients. Also provides convenience
 * methods for session-level messages ([publishStarted], [publishResult]).
 *
 * Thread-safe: all methods delegate to [BuildWebSocketServer.broadcast] which
 * handles its own synchronization.
 */
@Service(Service.Level.APP)
class BuildNotificationPublishService {

    /**
     * Notifies all connected clients that a new build session has started.
     *
     * @param projectName human-readable project name
     * @param buildId     opaque session identifier
     */
    fun publishStarted(projectName: String, buildId: String) {
        broadcast(BuildStartedPayload(buildId = buildId, projectName = projectName))
    }

    /**
     * Notifies all connected clients about the final build result.
     *
     * @param result aggregated build outcome with timing, status, and issues
     */
    fun publishResult(result: BuildResult) {
        broadcast(BuildResultPayload(result))
    }

    /**
     * Converts a typed [OutgoingBuildEvent] into the corresponding [WsPayload]
     * and broadcasts it to all connected clients.
     *
     * Called by [BuildEventPipeline][me.yuriisoft.buildnotify.build.pipeline.BuildEventPipeline]
     * for every event that passes through the handler chain.
     *
     * The `when` is exhaustive over the sealed class, so adding a new
     * [OutgoingBuildEvent] subclass will produce a compile error here until
     * a mapping branch is added.
     *
     * @param event typed build event produced by the pipeline
     */
    fun dispatch(event: OutgoingBuildEvent) {
        val payload: WsPayload = when (event) {
            is OutgoingBuildEvent.TaskStarted -> TaskStartedPayload(
                buildId = event.buildId,
                projectName = event.projectName,
                taskPath = event.taskPath,
            )
            is OutgoingBuildEvent.TaskFinished -> TaskFinishedPayload(
                buildId = event.buildId,
                projectName = event.projectName,
                taskPath = event.taskPath,
                status = event.status,
            )
            is OutgoingBuildEvent.Diagnostic -> BuildDiagnosticPayload(
                buildId = event.buildId,
                projectName = event.projectName,
                severity = event.severity,
                message = event.message,
                detail = event.detail,
                filePath = event.filePath,
                line = event.line,
                column = event.column,
            )
        }
        broadcast(payload)
    }

    private fun broadcast(payload: WsPayload) {
        val server = service<BuildWebSocketServer>()
        if (!server.isActive()) return
        server.broadcast(payload)
    }
}
