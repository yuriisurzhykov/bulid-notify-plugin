package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class IndeterminateLinearSpec(
    val barWidthRatio: Float = ProgressDefaults.IndeterminateBarWidthRatio,
    val overshootRatio: Float = ProgressDefaults.IndeterminateOvershootRatio,
    val durationMillis: Int = ProgressDefaults.IndeterminateDurationMillis,
    val easing: Easing = ProgressDefaults.IndeterminateEasing,
)