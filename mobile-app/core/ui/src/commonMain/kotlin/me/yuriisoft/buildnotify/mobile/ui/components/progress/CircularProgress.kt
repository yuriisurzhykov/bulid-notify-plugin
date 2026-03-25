package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular.CircularProgressMode
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Composable
fun CircularProgress(
    mode: CircularProgressMode,
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = ProgressDefaults.TrackAlpha),
    strokeWidth: Dp = BuildNotifyTheme.dimensions.component.progressCircularStrokeWidth,
    size: Dp = BuildNotifyTheme.dimensions.component.progressCircularSize,
) {
    val drawer = mode.rememberDrawer()
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { progressBarRangeInfo = mode.rangeInfo },
    ) {
        with(drawer) { draw(gradient, trackColor, stroke) }
    }
}
