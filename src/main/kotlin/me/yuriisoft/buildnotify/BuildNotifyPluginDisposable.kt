package me.yuriisoft.buildnotify

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import me.yuriisoft.buildnotify.build.BuildMonitorService

@Service(Service.Level.PROJECT)
class BuildNotifyPluginDisposable(private val project: Project) : Disposable {

    override fun dispose() {
        val basePath = project.basePath ?: return
        service<BuildMonitorService>().clearSessionsForProject(basePath)
    }

    companion object {
        fun getInstance(project: Project): BuildNotifyPluginDisposable {
            return project.getService(BuildNotifyPluginDisposable::class.java)
        }
    }
}
