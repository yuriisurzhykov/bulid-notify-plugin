package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class IndeterminateCircularSpec(
    val rotationDurationMillis: Int = ProgressDefaults.CircularRotationDurationMillis,
    val rotationEasing: Easing = ProgressDefaults.CircularRotationEasing,
    val sweepMinAngle: Float = ProgressDefaults.CircularSweepMinAngle,
    val sweepMaxAngle: Float = ProgressDefaults.CircularSweepMaxAngle,
    val sweepDurationMillis: Int = ProgressDefaults.CircularSweepDurationMillis,
    val sweepEasing: Easing = ProgressDefaults.CircularSweepEasing,
)

