package me.yuriisoft.buildnotify.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.yuriisoft.buildnotify.build.model.BuildResult
import me.yuriisoft.buildnotify.build.pipeline.DiagnosticSeverity
import me.yuriisoft.buildnotify.build.pipeline.TaskStatus

/**
 * Sealed hierarchy of all message payloads.
 *
 * Naming convention for @SerialName  →  "<namespace>.<action>"
 *   sys.*      — system / connection lifecycle
 *   build.*    — IDE build events, server → client
 *   cmd.*      — commands from client → server, and their results back
 *   agent.*    — AI-agent events, reserved for future use
 *
 * How to add a new message type:
 *   1. Add a data class that extends WsPayload.
 *   2. Annotate with @Serializable and @SerialName("namespace.name").
 *   3. Done — no registration, no factory, no changes to the serializer.
 */
@Serializable
sealed class WsPayload

// ─── System ──────────────────────────────────────────────────────────────────

/**
 * Sent by the server immediately after a client connects (onOpen).
 * Lets the client verify protocol compatibility before processing any other event.
 *
 * Client behaviour:
 *   - if protocolVersion > its own supported version → show "please update the app"
 *   - if protocolVersion < its own minimum supported → show "please update the plugin"
 *   - otherwise proceed normally
 */
@Serializable
@SerialName("sys.handshake")
data class HandshakePayload(
    val protocolVersion: Int = WsEnvelope.PROTOCOL_VERSION,
    /** Stable opaque ID for this IDE instance; changes only on full reinstall / new machine. */
    val instanceId: String,
    /** What this server can do right now; client hides UI for missing capabilities. */
    val capabilities: Set<Capability> = emptySet(),
) : WsPayload()

@Serializable
enum class Capability {
    BUILD_MONITOR,   // can receive build events
    BUILD_CONTROL,   // can send cmd.run_build / cmd.cancel_build
    AI_AGENT,        // can send cmd.trigger_agent (future)
}

/** Keep-alive ping from server. Client should reset its "connection lost" timer on receipt. */
@Serializable
@SerialName("sys.heartbeat")
data class HeartbeatPayload(
    val serverTime: Long = System.currentTimeMillis(),
) : WsPayload()

// ─── Build events (server → client) ──────────────────────────────────────────

@Serializable
@SerialName("build.started")
data class BuildStartedPayload(
    val buildId: String,
    val projectName: String,
) : WsPayload()

/**
 * A Gradle task has started executing.
 *
 * The client may render an animated "running" indicator until the matching
 * [TaskFinishedPayload] arrives for the same [buildId] + [taskPath].
 */
@Serializable
@SerialName("build.task_started")
data class TaskStartedPayload(
    val buildId: String,
    val projectName: String,
    val taskPath: String,
) : WsPayload()

/**
 * A Gradle task has finished executing.
 *
 * [status] tells the client the terminal outcome so it can color-code tasks
 * (e.g. grey for UP_TO_DATE, red for FAILED, green for SUCCESS).
 */
@Serializable
@SerialName("build.task_finished")
data class TaskFinishedPayload(
    val buildId: String,
    val projectName: String,
    val taskPath: String,
    val status: TaskStatus,
) : WsPayload()

/**
 * A compiler diagnostic (error or warning) detected during the build.
 *
 * When [filePath], [line], and [column] are present, the client can render
 * a clickable source reference. Otherwise, the diagnostic is project-level.
 */
@Serializable
@SerialName("build.diagnostic")
data class BuildDiagnosticPayload(
    val buildId: String,
    val projectName: String,
    val severity: DiagnosticSeverity,
    val message: String,
    val detail: String? = null,
    val filePath: String? = null,
    val line: Int? = null,
    val column: Int? = null,
) : WsPayload()

/** Final build result with aggregated statistics, issues, and timing. */
@Serializable
@SerialName("build.result")
data class BuildResultPayload(
    val result: BuildResult,
) : WsPayload()

// ─── Commands (client → server) ──────────────────────────────────────────────
//
// Every command is paired with a cmd.result response via WsEnvelope.correlationId:
//   client sends  → WsEnvelope(id = "X", payload = SomeCommand(...))
//   server replies → WsEnvelope(correlationId = "X", payload = CommandResultPayload(...))

@Serializable
@SerialName("cmd.cancel_build")
data class CancelBuildCommand(
    val buildId: String,
) : WsPayload()

@Serializable
@SerialName("cmd.run_build")
data class RunBuildCommand(
    val projectName: String,
    val tasks: List<String> = emptyList(),
) : WsPayload()

// ─── Command result (server → client) ────────────────────────────────────────

/**
 * Universal response to any client command.
 * Always arrives with WsEnvelope.correlationId = <id of the original command>.
 *
 * errorCode is a machine-readable constant (SNAKE_UPPER), suitable for i18n on the client.
 * message is a human-readable detail for logging/debugging.
 */
@Serializable
@SerialName("cmd.result")
data class CommandResultPayload(
    val status: CommandStatus,
    val errorCode: String? = null,
    val message: String? = null,
) : WsPayload()

@Serializable
enum class CommandStatus {
    /** Command queued or already in progress; result will follow as a build event. */
    ACCEPTED,
    /** Command understood but refused (unknown buildId, wrong state, insufficient capability). */
    REJECTED,
    /** Server-side error during execution. */
    FAILED,
}

// ─── Agent (future, skeleton only) ───────────────────────────────────────────
// Add payloads here when the AI-agent feature is ready.
// No code needs to change in the serializer or transport layer.
