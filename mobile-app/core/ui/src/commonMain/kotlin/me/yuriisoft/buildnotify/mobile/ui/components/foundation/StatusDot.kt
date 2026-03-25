package me.yuriisoft.buildnotify.mobile.ui.components.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = BuildNotifyTheme.dimensions.component.statusDotSize,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = color, shape = CircleShape),
    )
}
