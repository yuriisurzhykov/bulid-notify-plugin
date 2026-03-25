package me.yuriisoft.buildnotify.mobile.ui.components.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear.LinearProgressMode
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Composable
fun LabeledLinearProgress(
    mode: LinearProgressMode,
    modifier: Modifier = Modifier,
    gradient: GradientSpec = BuildNotifyTheme.brushes.progressGradient,
    trackColor: Color = BuildNotifyTheme.colors.outline.copy(alpha = ProgressDefaults.TrackAlpha),
    shape: Shape = BuildNotifyTheme.shapes.full,
    label: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BuildNotifyTheme.dimensions.spacing.tiny),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = label,
        )
        LinearProgress(
            mode = mode,
            gradient = gradient,
            trackColor = trackColor,
            shape = shape,
        )
    }
}
