package me.yuriisoft.buildnotify.build

import com.intellij.build.events.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent
import kotlinx.coroutines.*
import me.yuriisoft.buildnotify.build.mapper.BuildGraphEventMapper
import me.yuriisoft.buildnotify.build.mapper.BuildResultMapper
import me.yuriisoft.buildnotify.build.model.BuildIssue
import me.yuriisoft.buildnotify.build.model.BuildStatus
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service(Service.Level.APP)
class BuildMonitorService : Disposable {

    private val logger = thisLogger()

    private val sessionsByBuildId = ConcurrentHashMap<String, BuildSession>()
    private val locksByBuildId = ConcurrentHashMap<String, ReentrantLock>()
    private val cleanupStarted = AtomicBoolean(false)
    private val cleanupJob = SupervisorJob()
    private val cleanupScope = CoroutineScope(cleanupJob + Dispatchers.Default)

    private fun lockFor(buildId: String): ReentrantLock =
        locksByBuildId.computeIfAbsent(buildId) { ReentrantLock() }

    private inline fun <T> withBuildLock(buildId: String, block: () -> T): T =
        lockFor(buildId).withLock(block)

    private fun sessionTimeoutMs(): Long {
        val minutes = service<PluginSettingsState>().snapshot().sessionTimeoutMinutes.coerceAtLeast(1)
        return minutes * 60_000L
    }

    private fun normalizeProjectPath(path: String): String? {
        val p = path.trim()
        if (p.isEmpty()) return null
        return runCatching { Path.of(p).normalize().toString() }.getOrNull()
    }

    fun clearSessionsForProject(projectBasePath: String) {
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
        val timeoutMs = sessionTimeoutMs()
        val now = System.currentTimeMillis()
        for (buildId in ids) {
            withBuildLock(buildId) {
                val session = sessionsByBuildId[buildId] ?: return@withBuildLock
                if (now - session.startedAt <= timeoutMs) return@withBuildLock
                session.reportedStatus = BuildStatus.CANCELLED
                finalizeSessionLocked(buildId)
            }
        }
    }

    /** If session exists and is past timeout, finalizes as cancelled. Call only while holding [buildId] lock. */
    private fun evictIfStaleUnderLock(buildId: String) {
        val session = sessionsByBuildId[buildId] ?: return
        if (System.currentTimeMillis() - session.startedAt <= sessionTimeoutMs()) return
        session.reportedStatus = BuildStatus.CANCELLED
        finalizeSessionLocked(buildId)
    }

    fun onStart(projectPath: String, taskId: ExternalSystemTaskId) {
        getOrCreateSession(
            buildId = taskId.toString(),
            projectName = projectNameFrom(projectPath),
            projectPath = projectPath,
            startedAt = System.currentTimeMillis(),
        )
    }

    private fun getOrCreateSession(buildId: String, projectName: String, projectPath: String, startedAt: Long) {
        withBuildLock(buildId) {
            evictIfStaleUnderLock(buildId)
            if (sessionsByBuildId.containsKey(buildId)) return@withBuildLock
            val newSession = BuildSession(
                buildId = buildId,
                projectName = projectName,
                projectPath = projectPath,
                startedAt = startedAt,
            )
            sessionsByBuildId[buildId] = newSession
            publisher().publishStarted(
                projectName = newSession.projectName,
                buildId = newSession.buildId,
            )
            ensureCleanupRunning()
        }
    }

    fun onTaskOutput(taskId: ExternalSystemTaskId, text: String) {
        val id = taskId.toString()
        withBuildLock(id) {
            evictIfStaleUnderLock(id)
            val session = sessionsByBuildId[id] ?: return@withBuildLock
            val lines = normalizeOutput(text)
            if (lines.isEmpty()) return@withBuildLock
            lines.forEach { line -> session.rawOutput.add(line) }
        }
    }

    fun onTaskExecutionProgress(event: ExternalSystemTaskExecutionEvent) {
        val id = event.id.toString()
        withBuildLock(id) {
            evictIfStaleUnderLock(id)
            val session = sessionsByBuildId[id] ?: return@withBuildLock
            val title = event.progressEvent.displayName
                .takeIf { it.isNotBlank() }
                ?: event.progressEvent.descriptor.displayName
                    .takeIf { it.isNotBlank() }
                ?: return@withBuildLock
            session.externalProgressEvents.add(title)
        }
    }

    fun onBuildProgressEvent(projectBasePath: String?, buildId: Any, event: BuildEvent) {
        val buildIdStr = buildId.toString()
        val pathHint = projectBasePath?.takeIf { it.isNotBlank() }.orEmpty()

        withBuildLock(buildIdStr) {
            evictIfStaleUnderLock(buildIdStr)

            if (event is StartBuildEvent) {
                val title = event.buildDescriptor.title.takeIf { it.isNotBlank() } ?: "Android Build"
                val existing = sessionsByBuildId[buildIdStr]
                if (existing == null) {
                    val newSession = BuildSession(
                        buildId = buildIdStr,
                        projectName = title,
                        projectPath = pathHint,
                        startedAt = event.eventTime,
                    )
                    sessionsByBuildId[buildIdStr] = newSession
                    publisher().publishStarted(
                        projectName = newSession.projectName,
                        buildId = newSession.buildId,
                    )
                    ensureCleanupRunning()
                } else if (existing.projectPath.isBlank() && pathHint.isNotBlank()) {
                    existing.projectPath = pathHint
                }
            }

            val session = sessionsByBuildId[buildIdStr] ?: return@withBuildLock

            event.toIssueOrNull()?.let(session.issues::add)

            when (event) {
                is FinishBuildEvent -> session.treeResult = event.result.toBuildStatus()
                is FinishEvent -> {
                    if (event.parentId == null) {
                        session.treeResult = event.result.toBuildStatus()
                    }
                }
            }

            logBuildEvent(event)
            BuildGraphEventMapper.map(buildIdStr, event)
                ?.let(publisher()::publishGraphEvent)

            if (event is FinishBuildEvent || (event is FinishEvent && event.parentId == null)) {
                finalizeSessionLocked(buildIdStr)
            }
        }
    }

    private fun logBuildEvent(event: BuildEvent) {
        if (!logger.isDebugEnabled) return
        when (event) {
            is FinishEvent -> {
                val result = event.result
                logger.debug(
                    """
                    FINISH:
                    class=${event.javaClass.name}
                    message=${event.message}
                    hint=${event.hint}
                    description=${event.description}
                    resultClass=${result?.javaClass?.name}
                    failures=${(result as? FailureResult)?.failures?.size ?: 0}
                    """.trimIndent(),
                )

                if (result is FailureResult) {
                    result.failures.forEach { failure ->
                        logFailure(failure, 0)
                    }
                }
            }

            else -> {
                logger.debug(
                    """
                    EVENT:
                    class=${event.javaClass.name}
                    message=${event.message}
                    hint=${event.hint}
                    description=${event.description}
                    """.trimIndent(),
                )
            }
        }
    }

    private fun logFailure(failure: Failure, depth: Int) {
        if (!logger.isDebugEnabled) return
        val indent = "  ".repeat(depth)
        logger.debug(
            """
            ${indent}FAILURE:
            ${indent}class=${failure.javaClass.name}
            ${indent}message=${failure.message}
            ${indent}description=${failure.description}
            ${indent}causes=${failure.causes.size}
            """.trimIndent(),
        )
        failure.causes.forEach { cause ->
            logFailure(cause, depth + 1)
        }
    }

    fun onSuccess(taskId: ExternalSystemTaskId) {
        val id = taskId.toString()
        withBuildLock(id) {
            evictIfStaleUnderLock(id)
            sessionsByBuildId[id]?.reportedStatus = BuildStatus.SUCCESS
        }
    }

    fun onFailure(taskId: ExternalSystemTaskId, exception: Exception) {
        val id = taskId.toString()
        withBuildLock(id) {
            evictIfStaleUnderLock(id)
            val session = sessionsByBuildId[id] ?: return@withBuildLock
            session.reportedStatus = BuildStatus.FAILED
            session.issues.add(
                BuildIssue(
                    message = exception.message ?: exception.toString(),
                    severity = BuildIssue.Severity.ERROR,
                ),
            )
        }
        logger.warn("Gradle task failed: ${taskId.type} ${taskId.id}", exception)
    }

    fun onCancel(taskId: ExternalSystemTaskId) {
        val id = taskId.toString()
        withBuildLock(id) {
            evictIfStaleUnderLock(id)
            sessionsByBuildId[id]?.reportedStatus = BuildStatus.CANCELLED
        }
    }

    fun onEnd(taskId: ExternalSystemTaskId) {
        finalizeSession(taskId.toString())
    }

    private fun finalizeSession(buildId: String) {
        withBuildLock(buildId) {
            finalizeSessionLocked(buildId)
        }
    }

    /** Caller must hold the per-[buildId] lock. */
    private fun finalizeSessionLocked(buildId: String) {
        val session = sessionsByBuildId.remove(buildId) ?: return

        val settings = service<PluginSettingsState>().snapshot()
        val collectedIssues = session.issues
            .toList()
            .take(settings.maxIssuesPerNotification)

        val finalStatus = resolveFinalStatus(
            treeResult = session.treeResult,
            reportedStatus = session.reportedStatus,
            issues = collectedIssues,
            rawOutput = session.rawOutput.toList(),
        )

        val result = BuildResultMapper.map(
            buildId = session.buildId,
            projectName = session.projectName,
            status = finalStatus,
            startedAt = session.startedAt,
            collectedIssues = collectedIssues,
        )

        publisher().publishResult(result)
    }

    private fun resolveFinalStatus(
        treeResult: BuildStatus?,
        reportedStatus: BuildStatus?,
        issues: List<BuildIssue>,
        rawOutput: List<String>,
    ): BuildStatus {
        return when {
            treeResult != null -> treeResult
            issues.any { it.severity == BuildIssue.Severity.ERROR } -> BuildStatus.FAILED
            rawOutput.any { it.equals("BUILD FAILED", ignoreCase = true) } -> BuildStatus.FAILED
            reportedStatus != null -> reportedStatus
            else -> BuildStatus.FAILED
        }
    }

    private fun publisher(): BuildNotificationPublishService =
        service()

    private fun projectNameFrom(projectPath: String): String =
        runCatching {
            Path.of(projectPath).fileName?.toString().orEmpty()
        }.getOrDefault(projectPath)

    private fun normalizeOutput(text: String): List<String> =
        text.replace('\r', '\n')
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { it.take(2_000) }
            .toList()

    private fun buildDetailedMessage(message: String?, description: String?): String {
        val msg = message?.trim()?.takeIf { it.isNotEmpty() } ?: ""
        val desc = description?.trim()?.takeIf { it.isNotEmpty() } ?: ""

        if (msg.isEmpty() && desc.isEmpty()) return "Unknown build issue"
        if (msg == desc) return msg
        if (desc.startsWith(msg)) return desc
        if (msg.isNotEmpty() && desc.isNotEmpty()) return "$msg\n$desc"
        return msg.ifEmpty { desc }
    }

    private fun BuildEvent.toIssueOrNull(): BuildIssue? =
        when (this) {
            is FileMessageEvent -> when (kind) {
                MessageEvent.Kind.ERROR -> BuildIssue(
                    filePath = filePosition.file.path,
                    line = filePosition.startLine,
                    column = filePosition.startColumn,
                    message = buildDetailedMessage(message, description),
                    severity = BuildIssue.Severity.ERROR,
                )

                MessageEvent.Kind.WARNING -> BuildIssue(
                    filePath = filePosition.file.path,
                    line = filePosition.startLine,
                    column = filePosition.startColumn,
                    message = buildDetailedMessage(message, description),
                    severity = BuildIssue.Severity.WARNING,
                )

                else -> null
            }

            is MessageEvent -> when (kind) {
                MessageEvent.Kind.ERROR -> BuildIssue(
                    message = buildDetailedMessage(message, description),
                    severity = BuildIssue.Severity.ERROR,
                )

                MessageEvent.Kind.WARNING -> BuildIssue(
                    message = buildDetailedMessage(message, description),
                    severity = BuildIssue.Severity.WARNING,
                )

                else -> null
            }

            is FinishBuildEvent -> extractIssueFromResult(result, message)
            is FinishEvent -> extractIssueFromResult(result, message)
            is OutputBuildEvent -> {
                if (message.contains("BUILD FAILED", ignoreCase = true)) {
                    BuildIssue(
                        message = "Gradle reported BUILD FAILED",
                        severity = BuildIssue.Severity.ERROR,
                    )
                } else {
                    null
                }
            }

            else -> null
        }

    private fun extractIssueFromResult(
        result: EventResult,
        fallbackMessage: String,
    ): BuildIssue? =
        when (result) {
            is FailureResult -> {
                val failure = result.failures.firstOrNull()
                val failureMessage = failure?.let { buildDetailedMessage(it.message, it.description) }
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackMessage

                BuildIssue(
                    message = failureMessage,
                    severity = BuildIssue.Severity.ERROR,
                )
            }

            is SuccessResult -> null
            is SkippedResult -> null
            else -> null
        }

    private fun EventResult.toBuildStatus(): BuildStatus =
        when (this) {
            is SuccessResult -> BuildStatus.SUCCESS
            is FailureResult -> BuildStatus.FAILED
            is SkippedResult -> BuildStatus.CANCELLED
            else -> BuildStatus.FAILED
        }

    override fun dispose() {
        cleanupScope.cancel()
    }

    private class BuildSession(
        val buildId: String,
        val projectName: String,
        var projectPath: String,
        val startedAt: Long,
        val issues: ConcurrentLinkedQueue<BuildIssue> = ConcurrentLinkedQueue(),
        val rawOutput: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue(),
        val externalProgressEvents: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue(),
        var reportedStatus: BuildStatus? = null,
        var treeResult: BuildStatus? = null,
    )
}
