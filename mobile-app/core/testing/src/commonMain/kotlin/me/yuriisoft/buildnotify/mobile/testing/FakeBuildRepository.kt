package me.yuriisoft.buildnotify.mobile.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.yuriisoft.buildnotify.mobile.domain.model.BuildResult
import me.yuriisoft.buildnotify.mobile.domain.repository.IBuildRepository

/**
 * In-memory fake of [IBuildRepository] for unit tests.
 *
 * Call [emit] to push [BuildResult] events into the flow returned by
 * [observeEvents]. All command calls are recorded for assertion.
 */
class FakeBuildRepository : IBuildRepository {

    private val _events = MutableSharedFlow<BuildResult>()

    val cancelledBuilds: MutableList<String> = mutableListOf()
    val triggeredBuilds: MutableList<Pair<String, List<String>>> = mutableListOf()

    override fun observeEvents(host: String, port: Int): Flow<BuildResult> =
        _events.asSharedFlow()

    override suspend fun cancelBuild(buildId: String) {
        cancelledBuilds += buildId
    }

    override suspend fun runBuild(projectName: String, tasks: List<String>) {
        triggeredBuilds += projectName to tasks
    }

    suspend fun emit(result: BuildResult) {
        _events.emit(result)
    }
}
