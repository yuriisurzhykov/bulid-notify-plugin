package me.yuriisoft.buildnotify.mobile.ui.components.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.components.progress.CircularProgress
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular.CircularProgressMode
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun StatusIcon(
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    image: ImageResource? = null,
    contentDescription: TextResource? = null,
    size: Dp = BuildNotifyTheme.dimensions.icon.large,
    iconSize: Dp = BuildNotifyTheme.dimensions.icon.regular,
    loading: Boolean = false,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(size)
                .background(color = containerColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgress(
                    mode = CircularProgressMode.Indeterminate(),
                    size = iconSize,
                    strokeWidth = BuildNotifyTheme.dimensions.stroke.regular,
                )
            } else if (image != null) {
                Icon(
                    image = image,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}
