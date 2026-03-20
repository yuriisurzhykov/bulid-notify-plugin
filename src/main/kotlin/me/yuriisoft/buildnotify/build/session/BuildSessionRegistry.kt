package me.yuriisoft.buildnotify.build.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.*
import me.yuriisoft.buildnotify.build.model.BuildStatus
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Application-level registry that owns the lifecycle of every active [BuildSession].
 *
 * Provides per-build locking, CRUD operations on sessions, and automatic cleanup
 * of stale sessions that never received a finish event (e.g. IDE crash, killed
 * Gradle daemon). The cleanup coroutine runs in an internal [CoroutineScope] and
 * is cancelled on [dispose].
 *
 * **Thread safety:** every public method that touches a session must be called
 * inside [withBuildLock]. The lock is per-`buildId`, so concurrent builds do not
 * contend.
 *
 * ### Typical usage
 *
 * ```kotlin
 * val registry = service<BuildSessionRegistry>()
 * registry.withBuildLock(buildId) {
 *     val (session, isNew) = registry.getOrCreate(buildId, name, path, time)
 *     // ... mutate session ...
 *     registry.removeSession(buildId)
 * }
 * ```
 */
@Service(Service.Level.APP)
class BuildSessionRegistry : Disposable {

    private val sessionsByBuildId = ConcurrentHashMap<String, BuildSession>()
    private val locksByBuildId = ConcurrentHashMap<String, ReentrantLock>()
    private val cleanupStarted = AtomicBoolean(false)
    private val cleanupJob = SupervisorJob()
    private val cleanupScope = CoroutineScope(cleanupJob + Dispatchers.Default)

    fun lockFor(buildId: String): ReentrantLock =
        locksByBuildId.computeIfAbsent(buildId) { ReentrantLock() }

    /**
     * Executes [block] while holding the per-build [ReentrantLock].
     *
     * Automatically evicts a stale session (if any) before running the block,
     * so callers never see sessions that have timed out.
     *
     * @param buildId build identifier whose lock to acquire
     * @param block   action executed under the lock
     * @return the value produced by [block]
     */
    inline fun <T> withBuildLock(buildId: String, block: () -> T): T =
        lockFor(buildId).withLock {
            evictIfStaleUnderLock(buildId)
            block()
        }

    /**
     * Returns the session for [buildId], or `null` if none exists.
     *
     * **Must be called inside [withBuildLock].**
     */
    fun getSession(buildId: String): BuildSession? =
        sessionsByBuildId[buildId]

    /**
     * Returns an existing session for [buildId] or creates a new one.
     *
     * When a new session is created the cleanup coroutine is started lazily
     * (first session in the application lifetime).
     *
     * **Must be called inside [withBuildLock].**
     *
     * @return a pair of (session, `true` if newly created / `false` if existing)
     */
    fun getOrCreate(
        buildId: String,
        projectName: String,
        projectPath: String,
        startedAt: Long,
    ): Pair<BuildSession, Boolean> {
        val existing = sessionsByBuildId[buildId]
        if (existing != null) return existing to false
        val session = BuildSession(
            buildId = buildId,
            projectName = projectName,
            projectPath = projectPath,
            startedAt = startedAt,
        )
        sessionsByBuildId[buildId] = session
        ensureCleanupRunning()
        return session to true
    }

    /**
     * Removes and returns the session for [buildId], or `null` if absent.
     *
     * **Must be called inside [withBuildLock].**
     */
    fun removeSession(buildId: String): BuildSession? =
        sessionsByBuildId.remove(buildId)

    /**
     * Removes all sessions whose [BuildSession.projectPath] matches [projectBasePath]
     * (after path normalization).
     *
     * Called when a project is closed to avoid leaking stale sessions.
     */
    fun clearForProject(projectBasePath: String) {
        val normalized = normalizeProjectPath(projectBasePath) ?: return
        val ids = sessionsByBuildId.entries
            .filter { normalizeProjectPath(it.value.projectPath) == normalized }
            .map { it.key }
        for (buildId in ids) {
            withBuildLock(buildId) {
                val session = sessionsByBuildId[buildId] ?: return@withBuildLock
                if (normalizeProjectPath(session.projectPath) == normalized) {
                    sessionsByBuildId.remove(buildId)
                }
            }
        }
    }

    /**
     * Cancels the stale-session cleanup coroutine.
     *
     * Called by the platform when the application shuts down.
     */
    override fun dispose() {
        cleanupScope.cancel()
    }

    @PublishedApi
    internal fun evictIfStaleUnderLock(buildId: String) {
        val session = sessionsByBuildId[buildId] ?: return
        if (System.currentTimeMillis() - session.startedAt <= sessionTimeoutMs()) return
        session.reportedStatus = BuildStatus.CANCELLED
        sessionsByBuildId.remove(buildId)
    }

    private fun sessionTimeoutMs(): Long {
        val minutes = service<PluginSettingsState>().snapshot().sessionTimeoutMinutes.coerceAtLeast(1)
        return minutes * 60_000L
    }

    private fun ensureCleanupRunning() {
        if (!cleanupStarted.compareAndSet(false, true)) return
        cleanupScope.launch {
            while (isActive) {
                delay(60_000)
                purgeStaleSessions()
            }
        }
    }

    private fun purgeStaleSessions() {
        val ids = sessionsByBuildId.keys.toList()
        for (buildId in ids) {
            withBuildLock(buildId) {
                // evictIfStaleUnderLock already runs inside withBuildLock
            }
        }
    }

    private fun normalizeProjectPath(path: String): String? {
        val p = path.trim()
        if (p.isEmpty()) return null
        return runCatching { Path.of(p).normalize().toString() }.getOrNull()
    }
}
