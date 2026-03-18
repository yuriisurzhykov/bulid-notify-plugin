package me.yuriisoft.buildnotify.build.model

import kotlinx.serialization.Serializable

@Serializable
data class LiveBuildEvent(
    val buildId: String,
    val title: String,
    val message: String? = null,
    val kind: Kind,
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {

    @Serializable
    enum class Kind {
        STARTED,
        PROGRESS,
        OUTPUT,
        WARNING,
        ERROR,
        SUCCESS,
        FAILED,
        CANCELLED,
    }
}