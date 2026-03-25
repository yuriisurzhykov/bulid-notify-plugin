package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

private const val StartAngle = -90f

@Composable
fun CircularProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = 0.12f),
    strokeWidth: Dp = 4.dp,
    size: Dp = BuildNotifyTheme.dimensions.icon.large,
) {
    val coercedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress = animateFloatAsState(
        targetValue = coercedProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )
    val sweepAngle = remember(animatedProgress) {
        derivedStateOf { animatedProgress.value * 360f }
    }
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = coercedProgress,
                    range = 0f..1f,
                )
            },
    ) {
        val padding = stroke.width / 2f
        val arcSize = Size(
            this.size.width - stroke.width,
            this.size.height - stroke.width,
        )
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

        if (sweepAngle.value > 0f) {
            drawArc(
                brush = gradient.toBrush(this.size),
                startAngle = StartAngle,
                sweepAngle = sweepAngle.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
    }
}
