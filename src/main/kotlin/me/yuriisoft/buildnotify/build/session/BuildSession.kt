package me.yuriisoft.buildnotify.build.session

import me.yuriisoft.buildnotify.build.model.BuildIssue
import me.yuriisoft.buildnotify.build.model.BuildStatus

/**
 * Mutable state of a single in-progress build.
 *
 * Holds every piece of information accumulated between `StartBuildEvent` and
 * `FinishBuildEvent`: collected diagnostics, externally-reported status, and
 * the tree-result derived from the build event graph.
 *
 * **Thread safety:** all access must occur under the per-build lock managed
 * by [BuildSessionRegistry]. Fields use plain [MutableList] and nullable
 * `var`s — no internal synchronization.
 *
 * **Lifecycle:** created by [BuildSessionRegistry.getOrCreate], consumed and
 * removed by [BuildSessionRegistry.removeSession] when the build finalizes.
 *
 * @property buildId      opaque string that uniquely identifies this build run
 * @property projectName  human-readable project name (e.g. Gradle root project)
 * @property projectPath  absolute path to the project root; may be updated if
 *                        initially unknown (blank) and resolved later
 * @property startedAt    epoch millis when the build session was created
 * @property issues       diagnostics accumulated during the build
 * @property reportedStatus status reported by the external system listener
 *                          (e.g. Gradle: success / failure / cancel)
 * @property treeResult   status derived from the build event tree
 *                        (`FinishBuildEvent.result`)
 */
class BuildSession(
    val buildId: String,
    val projectName: String,
    var projectPath: String,
    val startedAt: Long,
    val issues: MutableList<BuildIssue> = mutableListOf(),
    var reportedStatus: BuildStatus? = null,
    var treeResult: BuildStatus? = null,
)
