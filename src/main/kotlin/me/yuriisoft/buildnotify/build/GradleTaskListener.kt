package me.yuriisoft.buildnotify.build

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener

/**
 * Extension-point listener that bridges Gradle task lifecycle callbacks
 * to [BuildMonitorService].
 *
 * Registered in `plugin.xml` as an `externalSystemTaskNotificationListener`.
 * The IDE instantiates this class automatically for every Gradle invocation.
 *
 * Each override delegates to a single [BuildMonitorService] method — no logic
 * lives here. The listener deliberately omits `onStatusChange` and
 * `onTaskOutput` as those signals are redundant with the build event tree.
 */
class GradleTaskListener : ExternalSystemTaskNotificationListener {

    override fun onStart(id: ExternalSystemTaskId, workingDir: String) {
        service<BuildMonitorService>().onStart(workingDir, id)
    }

    override fun onSuccess(id: ExternalSystemTaskId) {
        service<BuildMonitorService>().onSuccess(id)
    }

    override fun onFailure(id: ExternalSystemTaskId, exception: Exception) {
        service<BuildMonitorService>().onFailure(id, exception)
    }

    override fun onCancel(id: ExternalSystemTaskId) {
        service<BuildMonitorService>().onCancel(id)
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        service<BuildMonitorService>().onEnd(id)
    }
}
