package me.yuriisoft.buildnotify.build

import com.intellij.build.events.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent
import me.yuriisoft.buildnotify.build.model.BuildIssue
import me.yuriisoft.buildnotify.build.model.BuildStatus
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Service(Service.Level.APP)
class BuildMonitorService {

    private val logger = thisLogger()

    private val sessionsByTaskId = ConcurrentHashMap<ExternalSystemTaskId, BuildSession>()
    private val sessionsByBuildId = ConcurrentHashMap<String, BuildSession>()

    fun onStart(projectPath: String, taskId: ExternalSystemTaskId) {
        if (!taskId.isSupportedGradleTask()) return

        val buildId = taskId.toString()
        val session = BuildSession(
            buildId = buildId,
            projectName = projectNameFrom(projectPath),
            startedAt = System.currentTimeMillis(),
        )

        sessionsByTaskId[taskId] = session
        sessionsByBuildId[buildId] = session

        publisher().publishStarted(
            projectName = session.projectName,
            buildId = session.buildId,
        )
    }

    fun onTaskOutput(taskId: ExternalSystemTaskId, text: String) {
        if (!taskId.isSupportedGradleTask()) return

        val session = sessionsByTaskId[taskId] ?: return
        val lines = normalizeOutput(text)
        if (lines.isEmpty()) return

        lines.forEach { line ->
            session.rawOutput.add(line)
        }
    }

    fun onTaskExecutionProgress(event: ExternalSystemTaskExecutionEvent) {
        val session = sessionsByTaskId[event.id] ?: return

        val title = event.progressEvent.displayName
            .takeIf { it.isNotBlank() }
            ?: event.progressEvent.descriptor.displayName
                .takeIf { it.isNotBlank() }
            ?: return

        session.externalProgressEvents.add(title)
    }

    fun onBuildProgressEvent(taskId: ExternalSystemTaskId, event: BuildEvent) {
        val session = sessionsByTaskId[taskId] ?: return

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
        BuildGraphEventMapper.map(taskId, event)
            ?.let(publisher()::publishGraphEvent)
    }

    private fun logBuildEvent(event: BuildEvent) {
        when (event) {
            is FinishEvent -> {
                val result = event.result
                logger.warn(
                    """
                FINISH:
                class=${event.javaClass.name}
                message=${event.message}
                hint=${event.hint}
                description=${event.description}
                resultClass=${result?.javaClass?.name}
                failures=${(result as? FailureResult)?.failures?.size ?: 0}
                """.trimIndent()
                )

                if (result is FailureResult) {
                    result.failures.forEach { failure ->
                        logFailure(failure, 0)
                    }
                }
            }

            else -> {
                logger.warn(
                    """
                EVENT:
                class=${event.javaClass.name}
                message=${event.message}
                hint=${event.hint}
                description=${event.description}
                """.trimIndent()
                )
            }
        }
    }

    private fun logFailure(failure: Failure, depth: Int) {
        val indent = "  ".repeat(depth)
        logger.warn(
            """
        ${indent}FAILURE:
        ${indent}class=${failure.javaClass.name}
        ${indent}message=${failure.message}
        ${indent}description=${failure.description}
        ${indent}causes=${failure.causes.size}
        """.trimIndent()
        )

        failure.causes.forEach { cause ->
            logFailure(cause, depth + 1)
        }
    }

    fun onSuccess(taskId: ExternalSystemTaskId) {
        if (!taskId.isSupportedGradleTask()) return
        sessionsByTaskId[taskId]?.reportedStatus = BuildStatus.SUCCESS
    }

    fun onFailure(taskId: ExternalSystemTaskId, exception: Exception) {
        if (!taskId.isSupportedGradleTask()) return
        val session = sessionsByTaskId[taskId] ?: return
        session.reportedStatus = BuildStatus.FAILED

        session.issues.add(
            BuildIssue(
                message = exception.message ?: exception.toString(),
                severity = BuildIssue.Severity.ERROR
            )
        )
        logger.warn("Gradle task failed: ${taskId.type} ${taskId.id}", exception)
    }

    fun onCancel(taskId: ExternalSystemTaskId) {
        if (!taskId.isSupportedGradleTask()) return
        sessionsByTaskId[taskId]?.reportedStatus = BuildStatus.CANCELLED
    }

    fun onEnd(taskId: ExternalSystemTaskId) {
        if (!taskId.isSupportedGradleTask()) return

        val session = sessionsByTaskId.remove(taskId) ?: return
        sessionsByBuildId.remove(session.buildId)

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

    private fun publisher(): BuildNotificationPublisher =
        service()

    private fun ExternalSystemTaskId.isSupportedGradleTask(): Boolean =
        projectSystemId == GradleConstants.SYSTEM_ID &&
                type == ExternalSystemTaskType.EXECUTE_TASK

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
                // Достаем description из FailureResult — там может лежать StackTrace
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

    private data class BuildSession(
        val buildId: String,
        val projectName: String,
        val startedAt: Long,
        val issues: ConcurrentLinkedQueue<BuildIssue> = ConcurrentLinkedQueue(),
        val rawOutput: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue(),
        val externalProgressEvents: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue(),
        var reportedStatus: BuildStatus? = null,
        var treeResult: BuildStatus? = null,
    )
}