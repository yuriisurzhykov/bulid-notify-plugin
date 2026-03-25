package me.yuriisoft.buildnotify.mobile.ui.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.components.icon.Icon
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

private const val DisabledAlpha = 0.38f

@Composable
fun IconButton(
    image: ImageResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: TextResource? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = BuildNotifyTheme.colors.content.onElevated,
    size: Dp = 40.dp,
    iconSize: Dp = BuildNotifyTheme.dimensions.icon.regular,
) {
    val clickableModifier = if (enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer { alpha = if (enabled) 1f else DisabledAlpha }
                .background(color = containerColor, shape = CircleShape)
                .clip(CircleShape)
                .then(clickableModifier),
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
