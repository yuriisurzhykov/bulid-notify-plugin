package me.yuriisoft.buildnotify.mobile.data.protocol

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
