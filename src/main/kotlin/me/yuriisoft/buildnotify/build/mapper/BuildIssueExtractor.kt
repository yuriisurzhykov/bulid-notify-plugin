package me.yuriisoft.buildnotify.build.mapper

import com.intellij.build.events.*
import me.yuriisoft.buildnotify.build.model.BuildIssue

/**
 * Stateless extractor that converts IntelliJ [BuildEvent]s into domain [BuildIssue]s.
 *
 * Recognises four event families:
 *
 * | Event type          | Extracted as                              |
 * |---------------------|-------------------------------------------|
 * | [FileMessageEvent]  | ERROR / WARNING with file location        |
 * | [MessageEvent]      | ERROR / WARNING without file location     |
 * | [FinishBuildEvent], [FinishEvent] | ERROR from [FailureResult]   |
 * | [OutputBuildEvent]  | ERROR when output contains "BUILD FAILED" |
 *
 * All other events yield `null`.
 *
 * **Thread safety:** fully stateless — safe to call from any thread.
 *
 * ### Usage
 *
 * ```kotlin
 * val issue: BuildIssue? = BuildIssueExtractor.extract(event)
 * issue?.let(session.issues::add)
 * ```
 */
object BuildIssueExtractor {

    /**
     * Attempts to produce a [BuildIssue] from the given [event].
     *
     * @param event raw IntelliJ build event
     * @return a [BuildIssue] if the event carries diagnostic information, `null` otherwise
     */
    fun extract(event: BuildEvent): BuildIssue? =
        when (event) {
            is FileMessageEvent -> extractFromFileMessage(event)
            is MessageEvent -> extractFromMessage(event)
            is FinishBuildEvent -> extractFromResult(event.result, event.message)
            is FinishEvent -> extractFromResult(event.result, event.message)
            is OutputBuildEvent -> extractFromOutput(event)
            else -> null
        }

    private fun extractFromFileMessage(event: FileMessageEvent): BuildIssue? {
        val severity = event.kind.toSeverityOrNull() ?: return null
        return BuildIssue(
            filePath = event.filePosition.file.path,
            line = event.filePosition.startLine,
            column = event.filePosition.startColumn,
            message = buildDetailedMessage(event.message, event.description),
            severity = severity,
        )
    }

    private fun extractFromMessage(event: MessageEvent): BuildIssue? {
        val severity = event.kind.toSeverityOrNull() ?: return null
        return BuildIssue(
            message = buildDetailedMessage(event.message, event.description),
            severity = severity,
        )
    }

    private fun extractFromResult(result: EventResult, fallbackMessage: String): BuildIssue? {
        if (result !is FailureResult) return null
        val failure = result.failures.firstOrNull()
        val message = failure
            ?.let { buildDetailedMessage(it.message, it.description) }
            ?.takeIf { it.isNotBlank() }
            ?: fallbackMessage
        return BuildIssue(message = message, severity = BuildIssue.Severity.ERROR)
    }

    private fun extractFromOutput(event: OutputBuildEvent): BuildIssue? {
        if (!event.message.contains("BUILD FAILED", ignoreCase = true)) return null
        return BuildIssue(
            message = "Gradle reported BUILD FAILED",
            severity = BuildIssue.Severity.ERROR,
        )
    }

    private fun MessageEvent.Kind.toSeverityOrNull(): BuildIssue.Severity? =
        when (this) {
            MessageEvent.Kind.ERROR -> BuildIssue.Severity.ERROR
            MessageEvent.Kind.WARNING -> BuildIssue.Severity.WARNING
            else -> null
        }

    private fun buildDetailedMessage(message: String?, description: String?): String {
        val msg = message?.trim()?.takeIf { it.isNotEmpty() } ?: ""
        val desc = description?.trim()?.takeIf { it.isNotEmpty() } ?: ""

        if (msg.isEmpty() && desc.isEmpty()) return "Unknown build issue"
        if (msg == desc) return msg
        if (desc.startsWith(msg)) return desc
        if (msg.isNotEmpty() && desc.isNotEmpty()) return "$msg\n$desc"
        return msg.ifEmpty { desc }
    }
}
