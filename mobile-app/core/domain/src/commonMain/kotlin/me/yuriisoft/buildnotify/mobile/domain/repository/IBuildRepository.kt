package me.yuriisoft.buildnotify.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import me.yuriisoft.buildnotify.mobile.domain.model.BuildResult

/**
 * Manages the connection to a Build Notify plugin instance.
 *
 * Follows DIP: the domain layer depends only on this abstraction;
 * the concrete implementation (Ktor WebSocket) lives in `:core:data`.
 */
interface IBuildRepository {

    /**
     * Opens a persistent connection and emits every incoming [BuildResult].
     * The Flow completes when the connection is closed.
     * Cancelling the collector disconnects cleanly.
     */
    fun observeEvents(host: String, port: Int): Flow<BuildResult>

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
