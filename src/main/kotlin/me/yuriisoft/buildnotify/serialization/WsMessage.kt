package me.yuriisoft.buildnotify.serialization

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.yuriisoft.buildnotify.build.model.BuildGraphEvent
import me.yuriisoft.buildnotify.build.model.BuildResult

@Polymorphic
@Serializable
sealed class WsMessage {

    @Serializable
    @SerialName("heartbeat")
    data class Heartbeat(
        val timestamp: Long = System.currentTimeMillis(),
    ) : WsMessage()

    @Serializable
    @SerialName("build_started")
    data class BuildStarted(
        val projectName: String,
        val buildId: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : WsMessage()

    @Serializable
    @SerialName("build_graph_event")
    data class BuildGraphEventMessage(
        val event: BuildGraphEvent,
    ) : WsMessage()

    @Serializable
    @SerialName("build_result")
    data class BuildResultMessage(
        val result: BuildResult,
    ) : WsMessage()

    @Serializable
    @SerialName("command")
    data class Command(
        val command: CommandAction,
        val params: Map<String, String> = emptyMap(),
    ) : WsMessage() {

        @Serializable
        enum class CommandAction {
            RUN_BUILD,
            CANCEL_BUILD,
            TRIGGER_AI_AGENT,
        }
    }
}