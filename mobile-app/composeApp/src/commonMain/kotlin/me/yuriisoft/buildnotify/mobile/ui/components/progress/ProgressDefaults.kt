package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.unit.dp

object ProgressDefaults {

    const val TrackAlpha = 0.12f

    const val DeterminateDurationMillis = 300
    val DeterminateEasing: Easing = FastOutSlowInEasing

    const val IndeterminateBarWidthRatio = 0.3f
    const val IndeterminateOvershootRatio = 0.3f
    const val IndeterminateDurationMillis = 1200
    val IndeterminateEasing: Easing = FastOutSlowInEasing

    const val BufferAlpha = 0.24f

    const val StripedAngleDegrees = 45f
    const val StripedStripeAlpha = 0.2f
    const val StripedScrollDurationMillis = 800

    const val PulsingMinAlpha = 0.3f
    const val PulsingMaxAlpha = 1.0f
    const val PulsingDurationMillis = 1000

    const val PulsingMinScale = 0.85f
    const val PulsingMaxScale = 1.0f

    const val CircularRotationDurationMillis = 1100
    val CircularRotationEasing: Easing = LinearEasing
    const val CircularSweepMinAngle = 30f
    const val CircularSweepMaxAngle = 270f
    const val CircularSweepDurationMillis = 800
    val CircularSweepEasing: Easing = FastOutSlowInEasing

    const val SegmentedGapAngleDegrees = 6f

    val MultiRingSpacing = 3.dp

    const val ArcStartAngleDegrees = -90f
}
