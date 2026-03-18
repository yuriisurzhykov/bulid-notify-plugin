package me.yuriisoft.buildnotify.build

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent

class GradleTaskListener : ExternalSystemTaskNotificationListener {

    override fun onStart(id: ExternalSystemTaskId, workingDir: String) {
        service<BuildMonitorService>().onStart(workingDir, id)
    }

    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
        if (event is ExternalSystemTaskExecutionEvent) {
            service<BuildMonitorService>().onTaskExecutionProgress(event)
        }
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