package me.yuriisoft.buildnotify.build

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent
import me.yuriisoft.buildnotify.build.model.LiveBuildEvent

object BuildEventMapper {

    fun toLiveEvent(
        buildId: String,
        event: BuildEvent,
    ): LiveBuildEvent? =
        when (event) {
            is FileMessageEvent -> LiveBuildEvent(
                buildId = buildId,
                title = event.message,
                kind = event.kind.toLiveKind(),
                filePath = event.filePosition.file.path,
                line = event.filePosition.startLine,
                column = event.filePosition.startColumn,
            )

            is MessageEvent -> LiveBuildEvent(
                buildId = buildId,
                title = event.message,
                kind = event.kind.toLiveKind(),
            )

            else -> {
                val title = event.message.takeIf { it.isNotBlank() } ?: return null
                LiveBuildEvent(
                    buildId = buildId,
                    title = title,
                    kind = LiveBuildEvent.Kind.PROGRESS,
                )
            }
        }

    fun toLiveEvent(
        buildId: String,
        event: ExternalSystemTaskExecutionEvent,
    ): LiveBuildEvent? {
        val progressEvent = event.progressEvent
        val descriptor = progressEvent.descriptor

        val title = descriptor.displayName
            .takeIf { it.isNotBlank() }
            ?: progressEvent.displayName.takeIf { it.isNotBlank() }
            ?: return null

        val message = progressEvent.displayName.takeUnless { it.isBlank() || it == title }

        return LiveBuildEvent(
            buildId = buildId,
            title = title,
            message = message,
            kind = LiveBuildEvent.Kind.PROGRESS,
        )
    }

    private fun MessageEvent.Kind.toLiveKind(): LiveBuildEvent.Kind =
        when (this) {
            MessageEvent.Kind.ERROR -> LiveBuildEvent.Kind.ERROR
            MessageEvent.Kind.WARNING -> LiveBuildEvent.Kind.WARNING
            else -> LiveBuildEvent.Kind.PROGRESS
        }
}