package me.yuriisoft.buildnotify.build

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import me.yuriisoft.buildnotify.build.session.BuildSessionRegistry
import me.yuriisoft.buildnotify.serialization.ActiveBuildInfo
import me.yuriisoft.buildnotify.serialization.BuildSnapshotPayload

/**
 * Assembles a [BuildSnapshotPayload] from the current state of [BuildSessionRegistry].
 *
 * Called once per client immediately after the server receives `sys.hello`,
 * giving the client a consistent view of every build that is currently in-flight
 * so it does not miss events that started before it connected.
 *
 * Stateless — every call queries the registry afresh.
 */
@Service(Service.Level.APP)
class BuildSnapshotProvider {

    fun snapshot(): BuildSnapshotPayload =
        BuildSnapshotPayload(
            activeBuilds = service<BuildSessionRegistry>()
                .activeSessions()
                .map { session ->
                    ActiveBuildInfo(
                        buildId = session.buildId,
                        projectName = session.projectName,
                        startedAt = session.startedAt,
                        tasks = emptyList(),
                    )
                },
        )
}
