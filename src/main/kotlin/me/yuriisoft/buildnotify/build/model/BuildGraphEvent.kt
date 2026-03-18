package me.yuriisoft.buildnotify.build.model

import kotlinx.serialization.Serializable

@Serializable
data class BuildGraphEvent(
    val buildId: String,
    val nodeId: String,
    val parentNodeId: String?,
    val eventType: EventType,
    val title: String,
    val message: String? = null,
    val severity: Severity? = null,
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val result: ResultType? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {

    @Serializable
    enum class EventType {
        BUILD_START,
        NODE_START,
        NODE_PROGRESS,
        MESSAGE,
        FILE_MESSAGE,
        NODE_FINISH,
        OUTPUT,
    }

    @Serializable
    enum class Severity {
        INFO,
        WARNING,
        ERROR,
    }

    @Serializable
    enum class ResultType {
        SUCCESS,
        FAILED,
        CANCELLED,
    }
}