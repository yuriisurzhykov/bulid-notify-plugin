package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class MultiRingSpec(
    val ringSpacing: Dp = ProgressDefaults.MultiRingSpacing,
    val durationMillis: Int = ProgressDefaults.DeterminateDurationMillis,
    val easing: Easing = ProgressDefaults.DeterminateEasing,
)