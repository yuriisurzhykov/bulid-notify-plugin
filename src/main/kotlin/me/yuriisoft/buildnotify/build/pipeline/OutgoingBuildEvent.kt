package me.yuriisoft.buildnotify.build.pipeline

/**
 * Typed events produced by the build event pipeline and sent to connected clients.
 *
 * Each subclass represents a single, semantically meaningful build occurrence.
 * The client receives these events and renders them however it sees fit
 * (localization, theming, animation) — the server never formats display strings.
 *
 * To add a new event kind:
 * 1. Add a `data class` subclass here.
 * 2. Create a corresponding [BuildEventHandler][me.yuriisoft.buildnotify.build.pipeline.BuildEventHandler].
 * 3. Register the handler in [BuildEventPipeline][me.yuriisoft.buildnotify.build.pipeline.BuildEventPipeline].
 * 4. Add a matching `WsPayload` subclass and a mapping branch in
 *    [BuildNotificationPublishService.dispatch][me.yuriisoft.buildnotify.build.BuildNotificationPublishService.dispatch].
 */
sealed class OutgoingBuildEvent {

    /**
     * A Gradle task has started executing.
     *
     * Emitted once per task when execution begins; the client may show an
     * animated "running" indicator until the corresponding [TaskFinished] arrives.
     *
     * @property buildId   opaque ID that groups events of the same build session
     * @property projectName   human-readable project name (e.g. "E-commerce App")
     * @property taskPath  Gradle task path, e.g. `:app:compileDebugKotlin`
     * @property timestamp epoch millis when the IDE received the start event
     */
    data class TaskStarted(
        val buildId: String,
        val projectName: String,
        val taskPath: String,
        val timestamp: Long,
    ) : OutgoingBuildEvent()

    /**
     * A Gradle task has finished executing.
     *
     * Always paired with a preceding [TaskStarted] for the same [taskPath] within the
     * same [buildId]. The [status] tells the client the outcome so it can color-code
     * or filter tasks (e.g. grey for [TaskStatus.UP_TO_DATE], red for [TaskStatus.FAILED]).
     *
     * @property buildId   opaque build session ID
     * @property projectName   human-readable project name
     * @property taskPath  Gradle task path
     * @property status    terminal outcome of the task
     * @property timestamp epoch millis when the IDE received the finish event
     */
    data class TaskFinished(
        val buildId: String,
        val projectName: String,
        val taskPath: String,
        val status: TaskStatus,
        val timestamp: Long,
    ) : OutgoingBuildEvent()

    /**
     * A compiler diagnostic (error or warning) emitted during the build.
     *
     * May or may not be tied to a specific file location. When [filePath], [line],
     * and [column] are present, the client can render a clickable source reference.
     *
     * @property buildId   opaque build session ID
     * @property projectName   human-readable project name
     * @property severity  [DiagnosticSeverity.ERROR] or [DiagnosticSeverity.WARNING]
     * @property message   short summary (one line)
     * @property detail    optional multi-line explanation or stack trace
     * @property filePath  absolute path to the source file, if available
     * @property line      1-based line number, if available
     * @property column    1-based column number, if available
     */
    data class Diagnostic(
        val buildId: String,
        val projectName: String,
        val severity: DiagnosticSeverity,
        val message: String,
        val detail: String? = null,
        val filePath: String? = null,
        val line: Int? = null,
        val column: Int? = null,
    ) : OutgoingBuildEvent()
}

