package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import me.yuriisoft.buildnotify.mobile.ui.components.progress.ProgressDefaults
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear.DeterminateSpec
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Stable
interface CircularProgressMode {
    val rangeInfo: ProgressBarRangeInfo

    @Composable
    fun rememberDrawer(): CircularProgressDrawer

    @Immutable
    data class Determinate(
        val progress: Float,
        val spec: DeterminateSpec = DeterminateSpec(),
    ) : CircularProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)

        @Composable
        override fun rememberDrawer(): CircularProgressDrawer {
            val animated = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f) * 360f,
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            return remember(this) {
                CircularProgressDrawer { gradient, trackColor, stroke ->
                    drawTrackArc(trackColor, stroke)
                    if (animated.value > 0f) {
                        drawProgressArc(gradient, animated.value, stroke)
                    }
                }
            }
        }
    }

    @Immutable
    data class Indeterminate(
        val spec: IndeterminateCircularSpec = IndeterminateCircularSpec(),
    ) : CircularProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo.Indeterminate

        @Composable
        override fun rememberDrawer(): CircularProgressDrawer {
            val transition = rememberInfiniteTransition()
            val rotation = transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.rotationDurationMillis, easing = spec.rotationEasing),
                ),
            )
            val sweep = transition.animateFloat(
                initialValue = spec.sweepMinAngle,
                targetValue = spec.sweepMaxAngle,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.sweepDurationMillis, easing = spec.sweepEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            return remember(this) {
                CircularProgressDrawer { gradient, _, stroke ->
                    rotate(rotation.value) {
                        drawProgressArc(gradient, sweep.value, stroke)
                    }
                }
            }
        }
    }

    @Immutable
    data class Segmented(
        val progress: Float,
        val segments: Int,
        val spec: SegmentedSpec = SegmentedSpec(),
    ) : CircularProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f, segments)

        @Composable
        override fun rememberDrawer(): CircularProgressDrawer {
            val animated = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            val segmentCount = segments
            val gapAngle = spec.gapAngleDegrees
            return remember(this) {
                CircularProgressDrawer { gradient, trackColor, stroke ->
                    if (segmentCount <= 0) return@CircularProgressDrawer
                    val flatStroke = Stroke(width = stroke.width, cap = StrokeCap.Butt)
                    val segmentSweep = (360f - segmentCount * gapAngle) / segmentCount
                    val filled = animated.value * segmentCount

                    val padding = stroke.width / 2f
                    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                    val topLeft = Offset(padding, padding)

                    for (i in 0 until segmentCount) {
                        val startAngle = ProgressDefaults.ArcStartAngleDegrees +
                                i * (segmentSweep + gapAngle)

                        drawArc(
                            color = trackColor,
                            startAngle = startAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = flatStroke,
                        )

                        val segFill = (filled - i).coerceIn(0f, 1f)
                        if (segFill > 0f) {
                            drawArc(
                                brush = gradient.toBrush(size),
                                startAngle = startAngle,
                                sweepAngle = segmentSweep * segFill,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = flatStroke,
                            )
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class Pulsing(
        val spec: PulsingCircularSpec = PulsingCircularSpec(),
    ) : CircularProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo.Indeterminate

        @Composable
        override fun rememberDrawer(): CircularProgressDrawer {
            val transition = rememberInfiniteTransition()
            val alpha = transition.animateFloat(
                initialValue = spec.minAlpha,
                targetValue = spec.maxAlpha,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.durationMillis),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            val scaleValue = transition.animateFloat(
                initialValue = spec.minScale,
                targetValue = spec.maxScale,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.durationMillis),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            return remember(this) {
                CircularProgressDrawer { gradient, _, stroke ->
                    scale(scaleValue.value) {
                        val padding = stroke.width / 2f
                        val arcSize = Size(
                            size.width - stroke.width,
                            size.height - stroke.width,
                        )
                        drawArc(
                            brush = gradient.toBrush(size),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(padding, padding),
                            size = arcSize,
                            style = stroke,
                            alpha = alpha.value,
                        )
                    }
                }
            }
        }
    }

    @Immutable
    data class MultiRing(
        val rings: List<RingData>,
        val spec: MultiRingSpec = MultiRingSpec(),
    ) : CircularProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = if (rings.isNotEmpty()) {
                ProgressBarRangeInfo(rings.first().progress.coerceIn(0f, 1f), 0f..1f)
            } else {
                ProgressBarRangeInfo(0f, 0f..1f)
            }

        @Composable
        override fun rememberDrawer(): CircularProgressDrawer {
            val animatedSweeps = rings.mapIndexed { index, ring ->
                key(index) {
                    animateFloatAsState(
                        targetValue = ring.progress.coerceIn(0f, 1f) * 360f,
                        animationSpec = tween(spec.durationMillis, easing = spec.easing),
                    )
                }
            }
            val spacingPx = with(LocalDensity.current) { spec.ringSpacing.toPx() }
            val ringGradients = rings.map { it.gradient }
            return remember(this, spacingPx) {
                CircularProgressDrawer { _, trackColor, stroke ->
                    for (i in ringGradients.indices) {
                        val inset = i * (stroke.width + spacingPx)
                        val padding = stroke.width / 2f + inset
                        val arcSize = Size(
                            size.width - stroke.width - inset * 2f,
                            size.height - stroke.width - inset * 2f,
                        )
                        if (arcSize.width <= 0f || arcSize.height <= 0f) break
                        val topLeft = Offset(padding, padding)

                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = stroke,
                        )

                        val sweepAngle = animatedSweeps[i].value
                        if (sweepAngle > 0f) {
                            drawArc(
                                brush = ringGradients[i].toBrush(size),
                                startAngle = ProgressDefaults.ArcStartAngleDegrees,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = stroke,
                            )
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class Countdown(
        val progress: Float,
        val spec: DeterminateSpec = DeterminateSpec(),
    ) : CircularProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)

        @Composable
        override fun rememberDrawer(): CircularProgressDrawer {
            val animated = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f) * 360f,
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            return remember(this) {
                CircularProgressDrawer { gradient, trackColor, stroke ->
                    drawTrackArc(trackColor, stroke)
                    if (animated.value > 0f) {
                        drawProgressArc(gradient, animated.value, stroke)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawTrackArc(trackColor: Color, stroke: Stroke) {
    val padding = stroke.width / 2f
    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
    drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(padding, padding),
        size = arcSize,
        style = stroke,
    )
}

private fun DrawScope.drawProgressArc(
    gradient: GradientSpec,
    sweepAngle: Float,
    stroke: Stroke,
) {
    val padding = stroke.width / 2f
    val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
    drawArc(
        brush = gradient.toBrush(size),
        startAngle = ProgressDefaults.ArcStartAngleDegrees,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(padding, padding),
        size = arcSize,
        style = stroke,
    )
}
