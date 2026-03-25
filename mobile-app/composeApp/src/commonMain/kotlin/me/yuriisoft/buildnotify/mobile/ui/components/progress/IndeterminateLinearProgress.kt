package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Composable
fun IndeterminateLinearProgress(
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = 0.12f),
    shape: Shape = BuildNotifyTheme.shapes.full,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset = infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val height = BuildNotifyTheme.dimensions.component.progressBarHeight

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        drawRect(color = trackColor)
        val barWidth = size.width * 0.3f
        val startX = size.width * offset.value
        clipRect(
            left = startX.coerceAtLeast(0f),
            right = (startX + barWidth).coerceAtMost(size.width),
        ) {
            drawRect(brush = gradient.toBrush(size))
        }
    }
}
