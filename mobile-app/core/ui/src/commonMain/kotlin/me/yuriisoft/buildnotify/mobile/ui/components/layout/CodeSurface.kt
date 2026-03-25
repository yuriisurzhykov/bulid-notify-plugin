package me.yuriisoft.buildnotify.mobile.ui.components.layout

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun CodeSurface(
    modifier: Modifier = Modifier,
    color: Color = BuildNotifyTheme.colors.surface.code,
    contentColor: Color = BuildNotifyTheme.colors.content.onCode,
    shape: Shape = BuildNotifyTheme.shapes.small,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape,
        content = content,
    )
}
