package me.yuriisoft.buildnotify.mobile.ui.components.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = BuildNotifyTheme.colors.divider,
    thickness: Dp = BuildNotifyTheme.dimensions.stroke.thin,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color = color),
    )
}
