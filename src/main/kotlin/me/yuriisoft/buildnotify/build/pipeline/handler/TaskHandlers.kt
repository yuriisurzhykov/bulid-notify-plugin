package me.yuriisoft.buildnotify.build.pipeline.handler

import com.intellij.build.events.EventResult
import com.intellij.build.events.FailureResult
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.SkippedResult
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.StartEvent
import com.intellij.build.events.SuccessResult
import me.yuriisoft.buildnotify.build.pipeline.BuildEventContext
import me.yuriisoft.buildnotify.build.pipeline.BuildEventHandler
import me.yuriisoft.buildnotify.build.pipeline.OutgoingBuildEvent
import me.yuriisoft.buildnotify.build.pipeline.TaskStatus

/**
 * Converts a [StartEvent] (excluding [StartBuildEvent]) into a
 * [TaskStarted][OutgoingBuildEvent.TaskStarted] outgoing event.
 *
 * The client uses this to show a "running" indicator for the task.
 * [StartBuildEvent] is excluded because it represents the build session itself,
 * not an individual Gradle task.
 */
class TaskStartedHandler : BuildEventHandler {

    /**
     * @return a single-element list with [OutgoingBuildEvent.TaskStarted] when the
     *         event is a non-root [StartEvent], `null` otherwise
     */
    override fun handle(context: BuildEventContext): List<OutgoingBuildEvent>? {
        val event = context.event
        if (event !is StartEvent || event is StartBuildEvent) return null
        return listOf(
            OutgoingBuildEvent.TaskStarted(
                buildId = context.buildId,
                projectName = context.projectName,
                taskPath = event.message.cleanTaskPath(),
                timestamp = event.eventTime,
            ),
        )
    }
}

/**
 * Converts a [FinishEvent] (excluding [FinishBuildEvent] and root-level finishes)
 * into a [TaskFinished][OutgoingBuildEvent.TaskFinished] outgoing event.
 *
 * The [EventResult] is mapped to a [TaskStatus]:
 *
 * | EventResult    | TaskStatus      |
 * |----------------|-----------------|
 * | SuccessResult (isUpToDate=false) | [TaskStatus.SUCCESS]     |
 * | SuccessResult (isUpToDate=true)  | [TaskStatus.UP_TO_DATE]  |
 * | FailureResult  | [TaskStatus.FAILED]      |
 * | SkippedResult  | [TaskStatus.SKIPPED]     |
 * | other          | [TaskStatus.FAILED]      |
 */
class TaskFinishedHandler : BuildEventHandler {

    /**
     * @return a single-element list with [OutgoingBuildEvent.TaskFinished] when the
     *         event is a non-root [FinishEvent], `null` otherwise
     */
    override fun handle(context: BuildEventContext): List<OutgoingBuildEvent>? {
        val event = context.event
        if (event !is FinishEvent || event is FinishBuildEvent || event.parentId == null) return null
        return listOf(
            OutgoingBuildEvent.TaskFinished(
                buildId = context.buildId,
                projectName = context.projectName,
                taskPath = event.message.cleanTaskPath(),
                status = event.result.toTaskStatus(),
                timestamp = event.eventTime,
            ),
        )
    }
}

/**
 * Maps an IntelliJ [EventResult] to the pipeline's [TaskStatus] enum.
 *
 * [SuccessResult.isUpToDate] distinguishes between a task that actually ran
 * and one that was a no-op because its outputs were already current.
 */
private fun EventResult.toTaskStatus(): TaskStatus = when (this) {
    is SuccessResult -> if (isUpToDate) TaskStatus.UP_TO_DATE else TaskStatus.SUCCESS
    is FailureResult -> TaskStatus.FAILED
    is SkippedResult -> TaskStatus.SKIPPED
    else -> TaskStatus.FAILED
}

/**
 * Strips the `> Task ` prefix that IntelliJ prepends to task display names,
 * yielding a clean Gradle path like `:app:compileDebugKotlin`.
 */
private fun String.cleanTaskPath(): String = removePrefix("> Task ").trim()
