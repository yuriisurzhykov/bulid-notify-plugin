package me.yuriisoft.buildnotify

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import me.yuriisoft.buildnotify.build.session.BuildSessionRegistry

@Service(Service.Level.PROJECT)
class BuildNotifyPluginDisposable(private val project: Project) : Disposable {

    override fun dispose() {
        val basePath = project.basePath ?: return
        service<BuildSessionRegistry>().clearForProject(basePath)
    }

    companion object {
        fun getInstance(project: Project): BuildNotifyPluginDisposable {
            return project.getService(BuildNotifyPluginDisposable::class.java)
        }
    }
}
