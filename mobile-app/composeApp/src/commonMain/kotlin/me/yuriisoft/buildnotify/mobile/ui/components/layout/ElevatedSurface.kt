package me.yuriisoft.buildnotify.mobile.ui.components.layout

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun ElevatedSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = BuildNotifyTheme.colors.surface.elevated,
    contentColor: Color = BuildNotifyTheme.colors.content.onElevated,
    shape: Shape = BuildNotifyTheme.shapes.medium,
    elevation: Dp = BuildNotifyTheme.dimensions.elevation.small,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = color,
        contentColor = contentColor,
        shape = shape,
        elevation = elevation,
        content = content,
    )
}
