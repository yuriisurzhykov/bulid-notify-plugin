package me.yuriisoft.buildnotify.build.mapper

import com.intellij.build.events.EventResult
import com.intellij.build.events.FailureResult
import com.intellij.build.events.SkippedResult
import com.intellij.build.events.SuccessResult
import me.yuriisoft.buildnotify.build.model.BuildIssue
import me.yuriisoft.buildnotify.build.model.BuildResult
import me.yuriisoft.buildnotify.build.model.BuildStatus

/**
 * Stateless mapper that assembles final [BuildResult]s and resolves [BuildStatus].
 *
 * Combines multiple status signals (event tree result, external system callback,
 * collected issues) into a single deterministic outcome. Serves as the single
 * source of truth for "what status does the build get?".
 *
 * **Thread safety:** fully stateless — safe to call from any thread.
 *
 * ### Usage
 *
 * ```kotlin
 * val status = BuildResultMapper.resolveStatus(session.treeResult, session.reportedStatus, issues)
 * val result = BuildResultMapper.map(buildId, projectName, status, startedAt, issues)
 * ```
 */
object BuildResultMapper {

    /**
     * Determines the final [BuildStatus] from multiple signals.
     *
     * Resolution priority (first match wins):
     * 1. [treeResult] — status derived from the build event tree (most reliable)
     * 2. Presence of ERROR-severity issues in [issues]
     * 3. [reportedStatus] — status from the external system listener callback
     * 4. Fallback to [BuildStatus.FAILED] if no signal is available
     *
     * @param treeResult     status from `FinishBuildEvent.result`, or `null` if not yet received
     * @param reportedStatus status from Gradle task listener callbacks, or `null`
     * @param issues         diagnostics collected during the build
     * @return resolved build status
     */
    fun resolveStatus(
        treeResult: BuildStatus?,
        reportedStatus: BuildStatus?,
        issues: List<BuildIssue>,
    ): BuildStatus = when {
        treeResult != null -> treeResult
        issues.any { it.severity == BuildIssue.Severity.ERROR } -> BuildStatus.FAILED
        reportedStatus != null -> reportedStatus
        else -> BuildStatus.FAILED
    }

    /**
     * Converts an IntelliJ [EventResult] into the corresponding [BuildStatus].
     *
     * Used when processing `FinishBuildEvent` / `FinishEvent` to populate
     * [me.yuriisoft.buildnotify.build.session.BuildSession.treeResult].
     *
     * @param result the event result from the IDE build tree
     * @return mapped [BuildStatus]
     */
    fun toBuildStatus(result: EventResult): BuildStatus =
        when (result) {
            is SuccessResult -> BuildStatus.SUCCESS
            is FailureResult -> BuildStatus.FAILED
            is SkippedResult -> BuildStatus.CANCELLED
            else -> BuildStatus.FAILED
        }

    /**
     * Assembles a final [BuildResult] snapshot from session data.
     *
     * Calculates `finishedAt` and `durationMs` automatically from the current
     * wall-clock time.
     *
     * @param buildId         opaque build session identifier
     * @param projectName     human-readable project name
     * @param status          resolved final status (see [resolveStatus])
     * @param startedAt       epoch millis when the build started
     * @param collectedIssues diagnostics accumulated during the build
     * @return immutable build result ready for serialization and broadcast
     */
    fun map(
        buildId: String,
        projectName: String,
        status: BuildStatus,
        startedAt: Long,
        collectedIssues: List<BuildIssue>,
    ): BuildResult {
        val finishedAt = System.currentTimeMillis()

        return BuildResult(
            buildId = buildId,
            projectName = projectName,
            status = status,
            durationMs = finishedAt - startedAt,
            errors = collectedIssues.filter { it.severity == BuildIssue.Severity.ERROR },
            warnings = collectedIssues.filter { it.severity == BuildIssue.Severity.WARNING },
            startedAt = startedAt,
            finishedAt = finishedAt,
        )
    }
}
