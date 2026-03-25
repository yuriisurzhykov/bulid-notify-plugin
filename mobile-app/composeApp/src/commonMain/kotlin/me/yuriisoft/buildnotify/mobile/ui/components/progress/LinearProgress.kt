package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear.LinearProgressMode
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Composable
fun LinearProgress(
    mode: LinearProgressMode,
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = ProgressDefaults.TrackAlpha),
    shape: Shape = BuildNotifyTheme.shapes.full,
) {
    val drawer = mode.rememberDrawer()
    val height = BuildNotifyTheme.dimensions.component.progressBarHeight

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .semantics { progressBarRangeInfo = mode.rangeInfo },
    ) {
        with(drawer) { draw(gradient, trackColor) }
    }
}
