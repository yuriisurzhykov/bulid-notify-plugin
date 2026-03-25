package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear

import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class PulsingLinearSpec(
    val minAlpha: Float = ProgressDefaults.PulsingMinAlpha,
    val maxAlpha: Float = ProgressDefaults.PulsingMaxAlpha,
    val durationMillis: Int = ProgressDefaults.PulsingDurationMillis,
)