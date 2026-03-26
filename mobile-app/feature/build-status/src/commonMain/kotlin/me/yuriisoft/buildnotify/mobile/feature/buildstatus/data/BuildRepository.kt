package me.yuriisoft.buildnotify.mobile.feature.buildstatus.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.data.protocol.BuildResult
import me.yuriisoft.buildnotify.mobile.data.protocol.BuildResultPayload
import me.yuriisoft.buildnotify.mobile.data.protocol.CancelBuildCommand
import me.yuriisoft.buildnotify.mobile.data.protocol.RunBuildCommand
import me.yuriisoft.buildnotify.mobile.data.protocol.WsEnvelope
import me.yuriisoft.buildnotify.mobile.data.session.BuildSession
import me.yuriisoft.buildnotify.mobile.feature.buildstatus.domain.repository.IBuildRepository

/**
 * Concrete [IBuildRepository] backed by a Ktor WebSocket via [BuildSession].
 *
 * Translates between the wire protocol ([WsEnvelope] / [WsPayload]) and the
 * domain model ([BuildResult]).  Feature modules never see protocol types —
 * they depend only on the domain interface (DIP).
 */
@Inject
class BuildRepository(
    private val session: BuildSession,
) : IBuildRepository {

    override fun observeEvents(host: String, port: Int): Flow<BuildResult> =
        session.connect(host, port)
            .mapNotNull { payload -> (payload as? BuildResultPayload)?.result }

    override suspend fun cancelBuild(buildId: String) {
        session.send(WsEnvelope(payload = CancelBuildCommand(buildId)))
    }

    override suspend fun runBuild(projectName: String, tasks: List<String>) {
        session.send(WsEnvelope(payload = RunBuildCommand(projectName, tasks)))
    }
}
