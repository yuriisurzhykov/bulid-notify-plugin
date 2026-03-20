package me.yuriisoft.buildnotify.build.pipeline

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import me.yuriisoft.buildnotify.build.BuildNotificationPublishService
import me.yuriisoft.buildnotify.build.pipeline.handler.DiagnosticHandler
import me.yuriisoft.buildnotify.build.pipeline.handler.DiscardOutputHandler
import me.yuriisoft.buildnotify.build.pipeline.handler.DiscardSessionLifecycleHandler
import me.yuriisoft.buildnotify.build.pipeline.handler.FileDiagnosticHandler
import me.yuriisoft.buildnotify.build.pipeline.handler.TaskFinishedHandler
import me.yuriisoft.buildnotify.build.pipeline.handler.TaskStartedHandler

/**
 * Application-level service that owns the build-event processing pipeline.
 *
 * Acts as a thin adapter between the IntelliJ platform and the WebSocket layer:
 * it feeds each incoming [BuildEventContext] through a [BuildEventChain] and
 * dispatches the resulting [OutgoingBuildEvent]s to connected clients via
 * [BuildNotificationPublishService].
 *
 * ### Handler registration order
 *
 * The chain is evaluated top-to-bottom; the first handler that returns a
 * non-null result claims the event. Order matters:
 *
 * 1. **Discard handlers** — cheap filters that suppress noise before any
 *    real processing happens.
 * 2. **Task handlers** — convert start/finish events into typed task events.
 * 3. **Diagnostic handlers** — extract compiler errors and warnings.
 *    [FileDiagnosticHandler] must precede [DiagnosticHandler] because
 *    `FileMessageEvent` is a subclass of `MessageEvent`.
 *
 * ### Thread safety
 *
 * The pipeline itself is stateless; all state lives in the handlers (which are
 * also stateless) and in the publish service. Safe to call from any thread.
 *
 * ### Adding new event types
 *
 * 1. Create an [OutgoingBuildEvent] subclass.
 * 2. Implement a [BuildEventHandler].
 * 3. Register it in the [chain] below at the correct position.
 * 4. Add matching `WsPayload` and a branch in `dispatch()`.
 */
@Service(Service.Level.APP)
class BuildEventPipeline {

    private val chain = BuildEventChain(
        handlers = listOf(
            DiscardOutputHandler(),
            DiscardSessionLifecycleHandler(),
            TaskStartedHandler(),
            TaskFinishedHandler(),
            FileDiagnosticHandler(),
            DiagnosticHandler(),
        ),
    )

    /**
     * Processes a single build event through the handler chain and dispatches
     * all produced [OutgoingBuildEvent]s to connected WebSocket clients.
     *
     * @param context snapshot of the build event and its session metadata
     */
    fun process(context: BuildEventContext) {
        chain.process(context).forEach { event ->
            service<BuildNotificationPublishService>().dispatch(event)
        }
    }
}
