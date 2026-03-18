package me.yuriisoft.buildnotify.build.model

import kotlinx.serialization.Serializable

@Serializable
data class BuildResult(
    val buildId: String,
    val projectName: String,
    val status: BuildStatus,
    val durationMs: Long,
    val errors: List<BuildIssue> = emptyList(),
    val warnings: List<BuildIssue> = emptyList(),
    val startedAt: Long,
    val finishedAt: Long,
) {
    val isSuccessful: Boolean
        get() = status == BuildStatus.SUCCESS

    val errorCount: Int
        get() = errors.size

    val warningCount: Int
        get() = warnings.size
}