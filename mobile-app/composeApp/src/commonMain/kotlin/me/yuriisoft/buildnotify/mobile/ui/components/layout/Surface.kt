package me.yuriisoft.buildnotify.mobile.ui.components.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = BuildNotifyTheme.colors.surface.primary,
    contentColor: Color = BuildNotifyTheme.colors.content.primary,
    shape: Shape = BuildNotifyTheme.shapes.medium,
    border: BorderStroke? = null,
    elevation: Dp = BuildNotifyTheme.dimensions.elevation.none,
    content: @Composable BoxScope.() -> Unit,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    val borderModifier = if (border != null) {
        Modifier.border(border, shape)
    } else Modifier

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false
                )
                .then(borderModifier)
                .background(
                    color = color,
                    shape = shape
                )
                .clip(shape)
                .then(clickableModifier),
            content = content
        )
    }
}
