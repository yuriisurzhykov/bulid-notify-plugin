package me.yuriisoft.buildnotify.mobile.ui.components.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DiffLine {
    val text: String

    data class Added(override val text: String) : DiffLine
    data class Removed(override val text: String) : DiffLine
    data class Context(override val text: String) : DiffLine
}
