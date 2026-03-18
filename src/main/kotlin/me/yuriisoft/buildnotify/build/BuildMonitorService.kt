package me.yuriisoft.buildnotify.build

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.EventResult
import com.intellij.build.events.FailureResult
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.build.events.SkippedResult
import com.intellij.build.events.SuccessResult
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

    fun onTaskOutput(
        taskId: ExternalSystemTaskId,
        text: String,
    ) {
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

    fun onBuildProgressEvent(buildId: Any, event: BuildEvent) {
        val session = sessionsByBuildId[buildId.toString()] ?: return

        event.toIssueOrNull()?.let(session.issues::add)

        when (event) {
            is FinishBuildEvent -> session.treeResult = event.result.toBuildStatus()
            is FinishEvent -> {
                if (event.parentId == null) {
                    session.treeResult = event.result.toBuildStatus()
                }
            }
        }

        BuildGraphEventMapper.map(buildId, event)
            ?.let(publisher()::publishGraphEvent)
    }

    fun onSuccess(taskId: ExternalSystemTaskId) {
        if (!taskId.isSupportedGradleTask()) return
        sessionsByTaskId[taskId]?.reportedStatus = BuildStatus.SUCCESS
    }

    fun onFailure(taskId: ExternalSystemTaskId, exception: Exception) {
        if (!taskId.isSupportedGradleTask()) return
        sessionsByTaskId[taskId]?.reportedStatus = BuildStatus.FAILED
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

    private fun BuildEvent.toIssueOrNull(): BuildIssue? =
        when (this) {
            is FileMessageEvent -> when (kind) {
                MessageEvent.Kind.ERROR -> BuildIssue(
                    filePath = filePosition.file.path,
                    line = filePosition.startLine,
                    column = filePosition.startColumn,
                    message = message,
                    severity = BuildIssue.Severity.ERROR,
                )

                MessageEvent.Kind.WARNING -> BuildIssue(
                    filePath = filePosition.file.path,
                    line = filePosition.startLine,
                    column = filePosition.startColumn,
                    message = message,
                    severity = BuildIssue.Severity.WARNING,
                )

                else -> null
            }

            is MessageEvent -> when (kind) {
                MessageEvent.Kind.ERROR -> BuildIssue(
                    message = message,
                    severity = BuildIssue.Severity.ERROR,
                )

                MessageEvent.Kind.WARNING -> BuildIssue(
                    message = message,
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
                val failureMessage = result.failures
                    .firstOrNull()
                    ?.message
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