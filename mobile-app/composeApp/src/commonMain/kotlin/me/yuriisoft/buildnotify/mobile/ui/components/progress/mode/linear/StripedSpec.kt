package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults

@Immutable
data class StripedSpec(
    val stripeAngleDegrees: Float = ProgressDefaults.StripedAngleDegrees,
    val stripeAlpha: Float = ProgressDefaults.StripedStripeAlpha,
    val scrollDurationMillis: Int = ProgressDefaults.StripedScrollDurationMillis,
    val progressDurationMillis: Int = ProgressDefaults.DeterminateDurationMillis,
    val progressEasing: Easing = ProgressDefaults.DeterminateEasing,
)