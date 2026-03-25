package me.yuriisoft.buildnotify.mobile.data.protocol

import kotlinx.serialization.Serializable

/**
 * Terminal outcome of a single Gradle task, as reported by the plugin.
 *
 * [RUNNING] is transient — it appears in [TaskStartedPayload] only.
 * All other values are terminal and appear in [TaskFinishedPayload].
 */
@Serializable
enum class TaskStatus {
    RUNNING,
    SUCCESS,
    UP_TO_DATE,
    FAILED,
    SKIPPED,
}
