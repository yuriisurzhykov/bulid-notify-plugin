package me.yuriisoft.buildnotify.build.pipeline

import com.intellij.build.events.BuildEvent

/**
 * Immutable snapshot of a single build event together with its session metadata.
 *
 * Created by [BuildMonitorService][me.yuriisoft.buildnotify.build.BuildMonitorService]
 * for every [com.intellij.build.events.BuildEvent] received from the IDE and passed through the
 * [BuildEventPipeline].
 *
 * @property buildId     opaque string that uniquely identifies the build session
 * @property projectName human-readable project name (e.g. directory name or Gradle root project)
 * @property event       the raw IntelliJ platform build event
 */
data class BuildEventContext(
    val buildId: String,
    val projectName: String,
    val event: BuildEvent,
)