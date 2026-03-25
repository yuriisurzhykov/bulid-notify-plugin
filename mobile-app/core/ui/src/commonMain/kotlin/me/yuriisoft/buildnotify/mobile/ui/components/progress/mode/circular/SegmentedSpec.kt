package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class SegmentedSpec(
    val gapAngleDegrees: Float = ProgressDefaults.SegmentedGapAngleDegrees,
    val durationMillis: Int = ProgressDefaults.DeterminateDurationMillis,
    val easing: Easing = ProgressDefaults.DeterminateEasing,
)