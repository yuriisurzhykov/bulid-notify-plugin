package me.yuriisoft.buildnotify.mobile.data.protocol

import kotlinx.serialization.Serializable

/**
 * Severity level for compiler diagnostics received from the plugin.
 *
 * INFO-level IDE messages are discarded by the plugin pipeline;
 * only actionable diagnostics reach the mobile client.
 */
@Serializable
enum class DiagnosticSeverity {
    ERROR,
    WARNING,
}
