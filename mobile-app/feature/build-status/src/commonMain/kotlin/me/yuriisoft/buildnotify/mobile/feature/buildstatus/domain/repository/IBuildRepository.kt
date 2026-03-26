package me.yuriisoft.buildnotify.mobile.feature.buildstatus.domain.repository

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.data.protocol.BuildResult

/**
 * Observes build events from the active WebSocket session.
 *
 * Follows DIP: the domain layer depends only on this abstraction;
 * the concrete implementation subscribes to [ActiveSession.incoming].
 * Connection lifecycle is managed elsewhere ([ConnectionManager]).
 */
interface IBuildRepository {

    /**
     * Emits every incoming [BuildResult] from the shared session.
     * The flow never completes on its own — cancel the collector to stop.
     */
    fun observeEvents(): Flow<BuildResult>

    /**
     * Sends a command to cancel the build with [buildId].
     * No-op if not connected.
     */
    suspend fun cancelBuild(buildId: String)

    /**
     * Sends a command to trigger a build for [projectName] with optional [tasks].
     * No-op if not connected.
     */
    suspend fun runBuild(projectName: String, tasks: List<String> = emptyList())
}
