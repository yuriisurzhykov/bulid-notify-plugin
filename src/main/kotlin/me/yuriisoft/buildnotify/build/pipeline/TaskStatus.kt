package me.yuriisoft.buildnotify.build.pipeline

import kotlinx.serialization.Serializable

/**
 * Terminal outcome of a single Gradle task.
 *
 * Mirrors the possible results exposed by the IntelliJ build-event API:
 * - [RUNNING]    — task started, result unknown yet (only in [OutgoingBuildEvent.TaskStarted])
 * - [SUCCESS]    — task executed and produced new outputs
 * - [UP_TO_DATE] — task skipped because outputs are already current
 * - [FAILED]     — task execution failed
 * - [SKIPPED]    — task was skipped entirely (e.g. NO-SOURCE)
 */
@Serializable
enum class TaskStatus {
    RUNNING,
    SUCCESS,
    UP_TO_DATE,
    FAILED,
    SKIPPED,
}