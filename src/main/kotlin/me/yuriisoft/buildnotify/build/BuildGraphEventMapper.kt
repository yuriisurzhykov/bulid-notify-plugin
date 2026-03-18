package me.yuriisoft.buildnotify.build

import com.intellij.build.events.*
import me.yuriisoft.buildnotify.build.model.BuildGraphEvent

object BuildGraphEventMapper {

    fun map(
        buildId: Any,
        event: BuildEvent,
    ): BuildGraphEvent? {
        val graphBuildId = buildId.toString()
        val nodeId = event.id.toString()
        val parentNodeId = event.parentId?.toString()

        return when (event) {
            is StartBuildEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.BUILD_START,
                title = event.message,
            )

            is StartEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.NODE_START,
                title = event.message,
            )

            is FileMessageEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.FILE_MESSAGE,
                title = event.message,
                message = event.description,
                severity = event.kind.toSeverity(),
                filePath = event.filePosition.file.path,
                line = event.filePosition.startLine,
                column = event.filePosition.startColumn,
            )

            is MessageEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.MESSAGE,
                title = event.message,
                message = event.description,
                severity = event.kind.toSeverity(),
            )

            is FinishBuildEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.NODE_FINISH,
                title = event.message,
                result = event.result.toResultType(),
            )

            is FinishEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.NODE_FINISH,
                title = event.message,
                result = event.result.toResultType(),
            )

            is OutputBuildEvent -> BuildGraphEvent(
                buildId = graphBuildId,
                nodeId = nodeId,
                parentNodeId = parentNodeId,
                eventType = BuildGraphEvent.EventType.OUTPUT,
                title = event.message,
            )

            else -> null
        }
    }

    private fun MessageEvent.Kind.toSeverity(): BuildGraphEvent.Severity =
        when (this) {
            MessageEvent.Kind.ERROR -> BuildGraphEvent.Severity.ERROR
            MessageEvent.Kind.WARNING -> BuildGraphEvent.Severity.WARNING
            else -> BuildGraphEvent.Severity.INFO
        }

    private fun EventResult.toResultType(): BuildGraphEvent.ResultType =
        when (this) {
            is SuccessResult -> BuildGraphEvent.ResultType.SUCCESS
            is FailureResult -> BuildGraphEvent.ResultType.FAILED
            is SkippedResult -> BuildGraphEvent.ResultType.CANCELLED
            else -> BuildGraphEvent.ResultType.FAILED
        }
}