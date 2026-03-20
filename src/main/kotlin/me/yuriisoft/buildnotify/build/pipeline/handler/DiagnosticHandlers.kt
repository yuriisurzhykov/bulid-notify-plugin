package me.yuriisoft.buildnotify.build.pipeline.handler

import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import me.yuriisoft.buildnotify.build.pipeline.BuildEventContext
import me.yuriisoft.buildnotify.build.pipeline.BuildEventHandler
import me.yuriisoft.buildnotify.build.pipeline.DiagnosticSeverity
import me.yuriisoft.buildnotify.build.pipeline.OutgoingBuildEvent

/**
 * Converts a [FileMessageEvent] into a [Diagnostic][OutgoingBuildEvent.Diagnostic]
 * that includes source-file location (path, line, column).
 *
 * **Must be registered before [DiagnosticHandler]** in the handler chain because
 * [FileMessageEvent] extends [MessageEvent]. If the plain handler ran first, it
 * would consume file-level diagnostics without preserving the location metadata.
 *
 * Only ERROR and WARNING kinds are forwarded; INFO-level messages are ignored
 * as they produce too much noise for mobile clients.
 */
class FileDiagnosticHandler : BuildEventHandler {

    /**
     * @return a single-element list with [OutgoingBuildEvent.Diagnostic] (including file
     *         position) when the event is a diagnostic [FileMessageEvent], `null` otherwise
     */
    override fun handle(context: BuildEventContext): List<OutgoingBuildEvent>? {
        val event = context.event as? FileMessageEvent ?: return null
        if (!event.isDiagnostic()) return null
        return listOf(
            OutgoingBuildEvent.Diagnostic(
                buildId = context.buildId,
                projectName = context.projectName,
                severity = event.kind.toSeverity(),
                message = event.message,
                detail = event.description,
                filePath = event.filePosition.file.path,
                line = event.filePosition.startLine,
                column = event.filePosition.startColumn,
            ),
        )
    }
}

/**
 * Converts a plain [MessageEvent] (without file position) into a
 * [Diagnostic][OutgoingBuildEvent.Diagnostic].
 *
 * Handles compiler warnings/errors that are not tied to a specific file,
 * such as deprecation notices for whole modules or configuration-level issues.
 *
 * **Must be registered after [FileDiagnosticHandler]** — see its KDoc for rationale.
 */
class DiagnosticHandler : BuildEventHandler {

    /**
     * @return a single-element list with [OutgoingBuildEvent.Diagnostic] when the event
     *         is a diagnostic [MessageEvent], `null` otherwise
     */
    override fun handle(context: BuildEventContext): List<OutgoingBuildEvent>? {
        val event = context.event as? MessageEvent ?: return null
        if (!event.isDiagnostic()) return null
        return listOf(
            OutgoingBuildEvent.Diagnostic(
                buildId = context.buildId,
                projectName = context.projectName,
                severity = event.kind.toSeverity(),
                message = event.message,
                detail = event.description,
            ),
        )
    }
}

/**
 * Returns `true` when the message kind is actionable for the client
 * (ERROR or WARNING). INFO and other kinds are treated as noise.
 */
private fun MessageEvent.isDiagnostic(): Boolean =
    kind == MessageEvent.Kind.ERROR || kind == MessageEvent.Kind.WARNING

/**
 * Maps IntelliJ's [MessageEvent.Kind] to the pipeline's [DiagnosticSeverity].
 *
 * Only called after [isDiagnostic] returns `true`, so the `else` branch
 * defaults to WARNING as a safe fallback rather than throwing.
 */
private fun MessageEvent.Kind.toSeverity(): DiagnosticSeverity = when (this) {
    MessageEvent.Kind.ERROR -> DiagnosticSeverity.ERROR
    else -> DiagnosticSeverity.WARNING
}
