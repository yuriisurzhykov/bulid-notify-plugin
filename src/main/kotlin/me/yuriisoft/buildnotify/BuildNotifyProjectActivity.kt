package me.yuriisoft.buildnotify

import com.intellij.execution.ExecutionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import me.yuriisoft.buildnotify.build.BuildProgressBridge
import me.yuriisoft.buildnotify.discovery.MdnsAdvertiser
import me.yuriisoft.buildnotify.server.BuildWebSocketServer

/**
 * Entry point — runs once per project open.
 *
 * Responsibility: wire up services and start the server + mDNS if not yet active.
 * No business logic lives here.
 *
 * [GradleTaskListener] is registered as an extension point in plugin.xml,
 * so the IDE instantiates it directly — no manual subscription needed.
 */
class BuildNotifyProjectActivity : ProjectActivity {

    private val logger = thisLogger()

    override suspend fun execute(project: Project) {
        val server = service<BuildWebSocketServer>()
        val mdns = service<MdnsAdvertiser>()

        // APP-level services start only once, even when multiple projects are open.
        if (!server.isActive()) {
            server.start()
            mdns.start()
        }

        val disposable = BuildNotifyPluginDisposable.getInstance(project)
        val connection = project.messageBus.connect(disposable)
        val listener = BuildProgressBridge()

        connection.subscribe(
            ExecutionManager.EXECUTION_TOPIC,
            listener
        )

        logger.info("BuildNotify ready for project '${project.name}', server active=${server.isActive()}")
    }
}