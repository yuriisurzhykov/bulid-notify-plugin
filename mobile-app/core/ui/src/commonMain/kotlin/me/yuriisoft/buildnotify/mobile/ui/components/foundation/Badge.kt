package me.yuriisoft.buildnotify.mobile.ui.components.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = BuildNotifyTheme.colors.content.primary,
    shape: Shape = BuildNotifyTheme.shapes.full,
    content: @Composable RowScope.() -> Unit,
) {
    val component = BuildNotifyTheme.dimensions.component

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .height(component.badgeHeight)
                .background(color = containerColor, shape = shape)
                .padding(horizontal = component.badgePaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}
