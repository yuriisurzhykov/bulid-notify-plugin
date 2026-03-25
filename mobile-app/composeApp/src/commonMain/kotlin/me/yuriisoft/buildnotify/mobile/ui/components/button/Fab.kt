package me.yuriisoft.buildnotify.mobile.ui.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.components.icon.Icon
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun Fab(
    image: ImageResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: TextResource? = null,
    containerColor: Color = BuildNotifyTheme.colors.primary.main,
    contentColor: Color = BuildNotifyTheme.colors.primary.onMain,
    size: Dp = BuildNotifyTheme.dimensions.component.fabSize,
    iconSize: Dp = BuildNotifyTheme.dimensions.icon.regular,
    shape: Shape = BuildNotifyTheme.shapes.large,
    elevation: Dp = BuildNotifyTheme.dimensions.elevation.medium,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(size)
                .shadow(elevation = elevation, shape = shape, clip = false)
                .background(color = containerColor, shape = shape)
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                image = image,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
