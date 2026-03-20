package me.yuriisoft.buildnotify

import com.intellij.build.BuildProgressListener
import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import me.yuriisoft.buildnotify.build.BuildMonitorService
import me.yuriisoft.buildnotify.network.discovery.MdnsAdvertiser
import me.yuriisoft.buildnotify.network.server.BuildWebSocketServer

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

        if (!server.isActive()) {
            server.start()
            mdns.start()
        }

        val disposable = BuildNotifyPluginDisposable.getInstance(project)

        val buildProgressListener = BuildProgressListener { buildId, event: BuildEvent ->
            service<BuildMonitorService>().onBuildProgressEvent(
                project.basePath,
                buildId,
                event,
            )
        }

        @Suppress("UnstableApiUsage")
        runCatching {
            project.getService(BuildViewManager::class.java)
                ?.addListener(buildProgressListener, disposable)
        }.onFailure {
            logger.warn("Failed to attach to BuildViewManager", it)
        }

        @Suppress("UnstableApiUsage")
        runCatching {
            project.getService(SyncViewManager::class.java)
                ?.addListener(buildProgressListener, disposable)
        }.onFailure {
            logger.warn("Failed to attach to SyncViewManager", it)
        }

        logger.info("BuildNotify ready for project '${project.name}', server active=${server.isActive()}")
    }
}