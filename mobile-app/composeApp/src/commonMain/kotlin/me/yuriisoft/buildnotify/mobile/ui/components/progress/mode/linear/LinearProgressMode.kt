package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import kotlin.math.PI
import kotlin.math.tan

@Stable
interface LinearProgressMode {
    val rangeInfo: ProgressBarRangeInfo

    @Composable
    fun rememberDrawer(): LinearProgressDrawer

    @Immutable
    data class Determinate(
        val progress: Float,
        val spec: DeterminateSpec = DeterminateSpec(),
    ) : LinearProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)

        @Composable
        override fun rememberDrawer(): LinearProgressDrawer {
            val animated = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            return remember(this) {
                LinearProgressDrawer { gradient, trackColor ->
                    drawRect(color = trackColor)
                    val fillWidth = size.width * animated.value
                    if (fillWidth > 0f) {
                        clipRect(right = fillWidth) {
                            drawRect(brush = gradient.toBrush(size))
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class Indeterminate(
        val spec: IndeterminateLinearSpec = IndeterminateLinearSpec(),
    ) : LinearProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo.Indeterminate

        @Composable
        override fun rememberDrawer(): LinearProgressDrawer {
            val transition = rememberInfiniteTransition()
            val offset = transition.animateFloat(
                initialValue = -spec.overshootRatio,
                targetValue = 1f + spec.overshootRatio,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.durationMillis, easing = spec.easing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            val barRatio = spec.barWidthRatio
            return remember(this) {
                LinearProgressDrawer { gradient, trackColor ->
                    drawRect(color = trackColor)
                    val barWidth = size.width * barRatio
                    val startX = size.width * offset.value
                    clipRect(
                        left = startX.coerceAtLeast(0f),
                        right = (startX + barWidth).coerceAtMost(size.width),
                    ) {
                        drawRect(brush = gradient.toBrush(size))
                    }
                }
            }
        }
    }

    @Immutable
    data class Buffered(
        val progress: Float,
        val bufferProgress: Float,
        val spec: BufferedSpec = BufferedSpec(),
    ) : LinearProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)

        @Composable
        override fun rememberDrawer(): LinearProgressDrawer {
            val animatedProgress = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            val animatedBuffer = animateFloatAsState(
                targetValue = bufferProgress.coerceIn(0f, 1f),
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            val bufferAlpha = spec.bufferAlpha
            return remember(this) {
                LinearProgressDrawer { gradient, trackColor ->
                    drawRect(color = trackColor)
                    val bufferWidth = size.width * animatedBuffer.value
                    if (bufferWidth > 0f) {
                        clipRect(right = bufferWidth) {
                            drawRect(brush = gradient.toBrush(size), alpha = bufferAlpha)
                        }
                    }
                    val fillWidth = size.width * animatedProgress.value
                    if (fillWidth > 0f) {
                        clipRect(right = fillWidth) {
                            drawRect(brush = gradient.toBrush(size))
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class Stepped(
        val progress: Float,
        val steps: Int,
        val spec: SteppedSpec = SteppedSpec(),
    ) : LinearProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f, steps)

        @Composable
        override fun rememberDrawer(): LinearProgressDrawer {
            val gapPx = with(LocalDensity.current) {
                BuildNotifyTheme.dimensions.component.progressSegmentGap.toPx()
            }
            val animated = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(spec.durationMillis, easing = spec.easing),
            )
            val segmentCount = steps
            return remember(this, gapPx) {
                LinearProgressDrawer { gradient, trackColor ->
                    if (segmentCount <= 0) return@LinearProgressDrawer
                    val totalGap = (segmentCount - 1) * gapPx
                    val segW = (size.width - totalGap) / segmentCount
                    val filled = animated.value * segmentCount

                    for (i in 0 until segmentCount) {
                        val left = i * (segW + gapPx)
                        drawRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(segW, size.height),
                        )
                        val segFill = (filled - i).coerceIn(0f, 1f)
                        if (segFill > 0f) {
                            clipRect(left = left, right = left + segW * segFill) {
                                drawRect(brush = gradient.toBrush(size))
                            }
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class Striped(
        val progress: Float,
        val spec: StripedSpec = StripedSpec(),
    ) : LinearProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)

        @Composable
        override fun rememberDrawer(): LinearProgressDrawer {
            val animated = animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(spec.progressDurationMillis, easing = spec.progressEasing),
            )
            val transition = rememberInfiniteTransition()
            val scrollFraction = transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.scrollDurationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
            val angle = spec.stripeAngleDegrees
            val alpha = spec.stripeAlpha
            return remember(this) {
                LinearProgressDrawer { gradient, trackColor ->
                    drawRect(color = trackColor)
                    val fillWidth = size.width * animated.value
                    if (fillWidth > 0f) {
                        clipRect(right = fillWidth) {
                            drawRect(brush = gradient.toBrush(size))
                            drawBarberPoleStripes(angle, alpha, scrollFraction.value)
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class Pulsing(
        val spec: PulsingLinearSpec = PulsingLinearSpec(),
    ) : LinearProgressMode {

        override val rangeInfo: ProgressBarRangeInfo
            get() = ProgressBarRangeInfo.Indeterminate

        @Composable
        override fun rememberDrawer(): LinearProgressDrawer {
            val transition = rememberInfiniteTransition()
            val alpha = transition.animateFloat(
                initialValue = spec.minAlpha,
                targetValue = spec.maxAlpha,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.durationMillis),
                    repeatMode = RepeatMode.Reverse,
                ),
            )
            return remember(this) {
                LinearProgressDrawer { gradient, trackColor ->
                    drawRect(color = trackColor)
                    drawRect(brush = gradient.toBrush(size), alpha = alpha.value)
                }
            }
        }
    }
}

private fun DrawScope.drawBarberPoleStripes(
    angleDegrees: Float,
    alpha: Float,
    scrollFraction: Float,
) {
    val h = size.height
    val angleRad = (angleDegrees.toDouble() * PI / 180.0).toFloat()
    val tanAngle = tan(angleRad.toDouble()).toFloat()
    val dx = if (tanAngle > 0.001f) h / tanAngle else h
    val stripeW = dx * 0.5f
    val period = dx + stripeW
    val offset = scrollFraction * period

    val path = Path()
    var x = -period * 2f + offset
    while (x < size.width + period) {
        path.moveTo(x, h)
        path.lineTo(x + dx, 0f)
        path.lineTo(x + dx + stripeW, 0f)
        path.lineTo(x + stripeW, h)
        path.close()
        x += period
    }
    drawPath(path = path, color = Color.White.copy(alpha = alpha))
}
