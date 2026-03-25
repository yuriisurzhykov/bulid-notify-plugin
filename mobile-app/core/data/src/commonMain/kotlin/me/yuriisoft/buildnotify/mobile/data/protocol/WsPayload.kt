package me.yuriisoft.buildnotify.mobile.data.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.yuriisoft.buildnotify.mobile.domain.model.BuildResult
import kotlin.time.Clock

/**
 * Sealed hierarchy of all message payloads.
 *
 * Naming convention for @SerialName  →  "<namespace>.<action>"
 *   sys.*      — system / connection lifecycle
 *   build.*    — IDE build events, server → client
 *   cmd.*      — commands from client → server, and their results back
 *   agent.*    — AI-agent events, reserved for future use
 *
 * To add a new message type:
 *   1. Add a data class extending [WsPayload].
 *   2. Annotate with @Serializable and @SerialName("namespace.name").
 *   3. Done — no registration, no factory, no other changes needed.
 *
 * Mirror of the plugin's WsPayload — intentionally kept identical so the
 * two ends can evolve in lockstep without a separate shared artifact (YAGNI).
 */
@Serializable
sealed class WsPayload

// ─── System ──────────────────────────────────────────────────────────────────

/**
 * Sent by the server immediately after the client connects.
 * Lets the client verify protocol compatibility before processing any event.
 *
 * Client behaviour:
 *   - protocolVersion > own supported version  → "please update the app"
 *   - protocolVersion < own minimum supported  → "please update the plugin"
 *   - otherwise → proceed normally
 */
@Serializable
@SerialName("sys.handshake")
data class HandshakePayload(
    val protocolVersion: Int = WsEnvelope.PROTOCOL_VERSION,
    val instanceId: String,
    val capabilities: Set<Capability> = emptySet(),
) : WsPayload()

@Serializable
enum class Capability {
    BUILD_MONITOR,
    BUILD_CONTROL,
    AI_AGENT,
}

/** Keep-alive ping from server. Client resets its "connection lost" timer on receipt. */
@Serializable
@SerialName("sys.heartbeat")
data class HeartbeatPayload(
    val serverTime: Long = Clock.System.now().toEpochMilliseconds(),
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
 * The client may show an animated indicator until the matching
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
 * [status] drives colour-coding in the UI (grey = UP_TO_DATE, red = FAILED, etc.).
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
 * A compiler diagnostic detected during the build.
 * When [filePath], [line], and [column] are present the client can render
 * a tappable source reference; otherwise the diagnostic is project-level.
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
 * [errorCode] is a machine-readable SNAKE_UPPER constant, suitable for i18n.
 * [message] is a human-readable detail for logging / debugging.
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
    ACCEPTED,
    REJECTED,
    FAILED,
}

// ─── Agent (future, skeleton only) ───────────────────────────────────────────
// Add payloads here when the AI-agent feature is ready.
