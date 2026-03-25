package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular.CircularProgressMode
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Composable
fun LabeledCircularProgress(
    mode: CircularProgressMode,
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = ProgressDefaults.TrackAlpha),
    strokeWidth: Dp = BuildNotifyTheme.dimensions.component.progressCircularStrokeWidth,
    size: Dp = BuildNotifyTheme.dimensions.component.progressCircularSize,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgress(
            mode = mode,
            gradient = gradient,
            trackColor = trackColor,
            strokeWidth = strokeWidth,
            size = size,
        )
        content()
    }
}
