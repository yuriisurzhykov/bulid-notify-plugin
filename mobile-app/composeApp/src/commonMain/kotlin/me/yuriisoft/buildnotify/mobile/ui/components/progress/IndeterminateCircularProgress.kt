package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Composable
fun IndeterminateCircularProgress(
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    strokeWidth: Dp = 4.dp,
    size: Dp = BuildNotifyTheme.dimensions.icon.large,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
        ),
    )
    val sweepAngle = infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation.value }
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        val padding = stroke.width / 2f
        val arcSize = Size(
            this.size.width - stroke.width,
            this.size.height - stroke.width,
        )

        drawArc(
            brush = gradient.toBrush(this.size),
            startAngle = -90f,
            sweepAngle = sweepAngle.value,
            useCenter = false,
            topLeft = Offset(padding, padding),
            size = arcSize,
            style = stroke,
        )
    }
}
