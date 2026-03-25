package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
fun LinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = 0.12f),
    shape: Shape = BuildNotifyTheme.shapes.full,
) {
    val coercedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress = animateFloatAsState(
        targetValue = coercedProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )
    val height = BuildNotifyTheme.dimensions.component.progressBarHeight

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = coercedProgress,
                    range = 0f..1f,
                )
            },
    ) {
        drawRect(color = trackColor)
        val fillWidth = size.width * animatedProgress.value
        if (fillWidth > 0f) {
            clipRect(right = fillWidth) {
                drawRect(brush = gradient.toBrush(size))
            }
        }
    }
}
