package me.yuriisoft.buildnotify

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.APP, Service.Level.PROJECT)
class BuildNotifyPluginDisposable : Disposable {

    companion object {

        fun getInstance(): BuildNotifyPluginDisposable {
            return ApplicationManager.getApplication()
                .getService(BuildNotifyPluginDisposable::class.java)
        }

        fun getInstance(project: Project): BuildNotifyPluginDisposable {
            return project.getService(BuildNotifyPluginDisposable::class.java)
        }
    }

    override fun dispose() {

    }
}