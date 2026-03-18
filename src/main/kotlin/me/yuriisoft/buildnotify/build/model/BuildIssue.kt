package me.yuriisoft.buildnotify.build.model

import kotlinx.serialization.Serializable

@Serializable
data class BuildIssue(
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val message: String,
    val severity: Severity,
) {

    @Serializable
    enum class Severity {
        ERROR,
        WARNING,
    }
}