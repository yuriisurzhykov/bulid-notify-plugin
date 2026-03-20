package me.yuriisoft.buildnotify.build.pipeline

import kotlinx.serialization.Serializable

/**
 * Severity level for compiler diagnostics.
 *
 * Kept intentionally minimal — INFO-level messages from the IDE are discarded
 * by the pipeline as noise; only actionable diagnostics reach the client.
 */
@Serializable
enum class DiagnosticSeverity {
    ERROR,
    WARNING,
}