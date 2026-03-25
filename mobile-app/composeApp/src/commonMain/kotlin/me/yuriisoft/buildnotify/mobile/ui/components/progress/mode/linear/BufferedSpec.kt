package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class BufferedSpec(
    val bufferAlpha: Float = ProgressDefaults.BufferAlpha,
    val durationMillis: Int = ProgressDefaults.DeterminateDurationMillis,
    val easing: Easing = ProgressDefaults.DeterminateEasing,
)