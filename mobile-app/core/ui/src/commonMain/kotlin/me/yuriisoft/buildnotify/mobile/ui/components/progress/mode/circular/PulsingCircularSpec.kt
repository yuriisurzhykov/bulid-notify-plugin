package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class PulsingCircularSpec(
    val minAlpha: Float = ProgressDefaults.PulsingMinAlpha,
    val maxAlpha: Float = ProgressDefaults.PulsingMaxAlpha,
    val minScale: Float = ProgressDefaults.PulsingMinScale,
    val maxScale: Float = ProgressDefaults.PulsingMaxScale,
    val durationMillis: Int = ProgressDefaults.PulsingDurationMillis,
)