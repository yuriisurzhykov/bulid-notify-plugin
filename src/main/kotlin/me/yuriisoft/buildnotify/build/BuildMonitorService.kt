package me.yuriisoft.buildnotify.build

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import me.yuriisoft.buildnotify.build.mapper.BuildIssueExtractor
import me.yuriisoft.buildnotify.build.mapper.BuildResultMapper
import me.yuriisoft.buildnotify.build.model.BuildIssue
import me.yuriisoft.buildnotify.build.model.BuildStatus
import me.yuriisoft.buildnotify.build.pipeline.BuildEventContext
import me.yuriisoft.buildnotify.build.pipeline.BuildEventPipeline
import me.yuriisoft.buildnotify.build.session.BuildSession
import me.yuriisoft.buildnotify.build.session.BuildSessionRegistry
import me.yuriisoft.buildnotify.settings.PluginSettingsState
import java.nio.file.Path

/**
 * Thin coordinator that receives build lifecycle callbacks and delegates
 * all processing to specialised collaborators.
 *
 * Does **not** own any state — sessions live in [BuildSessionRegistry],
 * issue extraction is handled by [BuildIssueExtractor], status resolution and
 * result assembly by [BuildResultMapper], and event streaming by
 * [BuildEventPipeline].
 *
 * ### Entry points
 *
 * - [onStart] / [onSuccess] / [onFailure] / [onCancel] / [onEnd] — called by
 *   [GradleTaskListener] (external system listener).
 * - [onBuildProgressEvent] — called by [BuildNotifyProjectActivity][me.yuriisoft.buildnotify.BuildNotifyProjectActivity]
 *   via `BuildViewManager` / `SyncViewManager`.
 *
 * ### Thread safety
 *
 * Every callback acquires a per-build lock via [BuildSessionRegistry.withBuildLock],
 * so concurrent events for the same build are serialised, while different builds
 * proceed in parallel.
 */
@Service(Service.Level.APP)
class BuildMonitorService {

    private val logger = thisLogger()

    /**
     * Registers a new build session when a Gradle task starts.
     *
     * Called by [GradleTaskListener.onStart].
     */
    fun onStart(projectPath: String, taskId: ExternalSystemTaskId) {
        val id = taskId.toString()
        val registry = service<BuildSessionRegistry>()
        registry.withBuildLock(id) {
            val (session, isNew) = registry.getOrCreate(
                buildId = id,
                projectName = projectNameFrom(projectPath),
                projectPath = projectPath,
                startedAt = System.currentTimeMillis(),
            )
            if (isNew) {
                publisher().publishStarted(
                    projectName = session.projectName,
                    buildId = session.buildId,
                )
            }
        }
    }

    /**
     * Processes a single build progress event from the IDE build tree.
     *
     * Handles session creation on [StartBuildEvent], issue extraction,
     * tree-result tracking, pipeline dispatch, and session finalization
     * on terminal events.
     *
     * @param projectBasePath project root path hint (may be `null` or blank)
     * @param buildId         opaque build identifier from the platform
     * @param event           raw IntelliJ build event
     */
    fun onBuildProgressEvent(projectBasePath: String?, buildId: Any, event: BuildEvent) {
        val id = buildId.toString()
        val pathHint = projectBasePath?.takeIf { it.isNotBlank() }.orEmpty()
        val registry = service<BuildSessionRegistry>()

        registry.withBuildLock(id) {
            if (event is StartBuildEvent) {
                handleBuildStart(id, event, pathHint, registry)
            }

            val session = registry.getSession(id) ?: return@withBuildLock

            BuildIssueExtractor.extract(event)?.let(session.issues::add)
            trackTreeResult(session, event)

            service<BuildEventPipeline>().process(
                BuildEventContext(id, session.projectName, event),
            )

            if (event is FinishBuildEvent || (event is FinishEvent && event.parentId == null)) {
                finalizeSessionLocked(id, registry)
            }
        }
    }

    /**
     * Marks the build as successful.
     *
     * Called by [GradleTaskListener.onSuccess].
     */
    fun onSuccess(taskId: ExternalSystemTaskId) {
        val id = taskId.toString()
        val registry = service<BuildSessionRegistry>()
        registry.withBuildLock(id) {
            registry.getSession(id)?.reportedStatus = BuildStatus.SUCCESS
        }
    }

    /**
     * Marks the build as failed and records the exception as an ERROR issue.
     *
     * Called by [GradleTaskListener.onFailure].
     */
    fun onFailure(taskId: ExternalSystemTaskId, exception: Exception) {
        val id = taskId.toString()
        val registry = service<BuildSessionRegistry>()
        registry.withBuildLock(id) {
            val session = registry.getSession(id) ?: return@withBuildLock
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

    /**
     * Marks the build as cancelled.
     *
     * Called by [GradleTaskListener.onCancel].
     */
    fun onCancel(taskId: ExternalSystemTaskId) {
        val id = taskId.toString()
        val registry = service<BuildSessionRegistry>()
        registry.withBuildLock(id) {
            registry.getSession(id)?.reportedStatus = BuildStatus.CANCELLED
        }
    }

    /**
     * Finalizes the build session and broadcasts the result.
     *
     * Called by [GradleTaskListener.onEnd] as the last callback in the
     * external system task lifecycle.
     */
    fun onEnd(taskId: ExternalSystemTaskId) {
        val id = taskId.toString()
        val registry = service<BuildSessionRegistry>()
        registry.withBuildLock(id) {
            finalizeSessionLocked(id, registry)
        }
    }

    private fun handleBuildStart(
        buildId: String,
        event: StartBuildEvent,
        pathHint: String,
        registry: BuildSessionRegistry,
    ) {
        val title = event.buildDescriptor.title.takeIf { it.isNotBlank() } ?: "Android Build"
        val existing = registry.getSession(buildId)
        if (existing == null) {
            val (session, _) = registry.getOrCreate(
                buildId = buildId,
                projectName = title,
                projectPath = pathHint,
                startedAt = event.eventTime,
            )
            publisher().publishStarted(
                projectName = session.projectName,
                buildId = session.buildId,
            )
        } else if (existing.projectPath.isBlank() && pathHint.isNotBlank()) {
            existing.projectPath = pathHint
        }
    }

    private fun trackTreeResult(session: BuildSession, event: BuildEvent) {
        when (event) {
            is FinishBuildEvent -> session.treeResult = BuildResultMapper.toBuildStatus(event.result)
            is FinishEvent -> {
                if (event.parentId == null) {
                    session.treeResult = BuildResultMapper.toBuildStatus(event.result)
                }
            }
        }
    }

    private fun finalizeSessionLocked(buildId: String, registry: BuildSessionRegistry) {
        val session = registry.removeSession(buildId) ?: return

        val settings = service<PluginSettingsState>().snapshot()
        val collectedIssues = session.issues
            .toList()
            .take(settings.maxIssuesPerNotification)

        val finalStatus = BuildResultMapper.resolveStatus(
            treeResult = session.treeResult,
            reportedStatus = session.reportedStatus,
            issues = collectedIssues,
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

    private fun publisher(): BuildNotificationPublishService = service()

    private fun projectNameFrom(projectPath: String): String =
        runCatching {
            Path.of(projectPath).fileName?.toString().orEmpty()
        }.getOrDefault(projectPath)
}
