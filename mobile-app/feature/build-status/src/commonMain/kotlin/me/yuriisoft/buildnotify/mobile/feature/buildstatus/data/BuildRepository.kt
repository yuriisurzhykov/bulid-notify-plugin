package me.yuriisoft.buildnotify.mobile.feature.buildstatus.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.data.protocol.BuildResult
import me.yuriisoft.buildnotify.mobile.data.protocol.BuildResultPayload
import me.yuriisoft.buildnotify.mobile.data.protocol.CancelBuildCommand
import me.yuriisoft.buildnotify.mobile.data.protocol.RunBuildCommand
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.feature.buildstatus.domain.repository.IBuildRepository
import me.yuriisoft.buildnotify.mobile.network.connection.ActiveSession

/**
 * Concrete [IBuildRepository] backed by the shared [ActiveSession].
 *
 * Subscribes to [ActiveSession.incoming], filtering for [BuildResultPayload]
 * and mapping to the domain [BuildResult]. Commands are sent through
 * [ActiveSession.send]. No connection management here — SRP.
 */
@Inject
class BuildRepository(
    private val session: ActiveSession,
) : IBuildRepository {

    override fun observeEvents(): Flow<BuildResult> =
        session.incoming
            .filterIsInstance<BuildResultPayload>()
            .map { it.result }

    override suspend fun cancelBuild(buildId: String) {
        session.send(WsEnvelope(payload = CancelBuildCommand(buildId)))
    }

    override suspend fun runBuild(projectName: String, tasks: List<String>) {
        session.send(WsEnvelope(payload = RunBuildCommand(projectName, tasks)))
    }
}
