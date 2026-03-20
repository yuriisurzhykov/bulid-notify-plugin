package me.yuriisoft.buildnotify.build.pipeline.handler

import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.build.events.StartBuildEvent
import me.yuriisoft.buildnotify.build.pipeline.BuildEventContext
import me.yuriisoft.buildnotify.build.pipeline.BuildEventHandler
import me.yuriisoft.buildnotify.build.pipeline.OutgoingBuildEvent

/**
 * Discards [OutputBuildEvent]s (raw console lines).
 *
 * The client does not need unstructured text — all meaningful information
 * is conveyed by typed events ([TaskStarted][OutgoingBuildEvent.TaskStarted],
 * [Diagnostic][OutgoingBuildEvent.Diagnostic], etc.).
 * Suppressing output events also reduces WebSocket traffic significantly
 * on large builds.
 *
 * If a "raw logs" feature is needed later, replace this handler with one
 * that batches output lines into a dedicated `ConsoleOutput` event.
 */
class DiscardOutputHandler : BuildEventHandler {

    /** @return empty list (discard) for [OutputBuildEvent], `null` otherwise */
    override fun handle(context: BuildEventContext): List<OutgoingBuildEvent>? =
        if (context.event is OutputBuildEvent) emptyList() else null
}

/**
 * Discards session-lifecycle events that carry no task-level information:
 *
 * - [StartBuildEvent] — build session opened (already handled by `build.started` payload)
 * - [FinishBuildEvent] — build session closed (already handled by `build.result` payload)
 * - Root [FinishEvent] (parentId == null) — synthetic root-node close event
 *
 * These events are meaningful for the session bookkeeping in
 * [BuildMonitorService][me.yuriisoft.buildnotify.build.BuildMonitorService],
 * but they add no value for the streaming pipeline because the client already
 * receives explicit `build.started` and `build.result` messages.
 */
class DiscardSessionLifecycleHandler : BuildEventHandler {

    /**
     * @return empty list (discard) for session-level lifecycle events,
     *         `null` for everything else
     */
    override fun handle(context: BuildEventContext): List<OutgoingBuildEvent>? = when {
        context.event is StartBuildEvent -> emptyList()
        context.event is FinishBuildEvent -> emptyList()
        context.event is FinishEvent && context.event.parentId == null -> emptyList()
        else -> null
    }
}
