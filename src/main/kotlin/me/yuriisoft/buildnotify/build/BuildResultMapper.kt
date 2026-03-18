package me.yuriisoft.buildnotify.build

import me.yuriisoft.buildnotify.build.model.BuildIssue
import me.yuriisoft.buildnotify.build.model.BuildResult
import me.yuriisoft.buildnotify.build.model.BuildStatus

object BuildResultMapper {

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