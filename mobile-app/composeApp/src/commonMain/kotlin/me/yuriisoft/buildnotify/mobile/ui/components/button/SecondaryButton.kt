package me.yuriisoft.buildnotify.mobile.ui.components.button

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

private const val DisabledAlpha = 0.38f

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = BuildNotifyTheme.colors.outline,
    contentColor: Color = BuildNotifyTheme.colors.content.primary,
    shape: Shape = BuildNotifyTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = BuildNotifyTheme.dimensions.spacing.xLarge,
        vertical = BuildNotifyTheme.dimensions.spacing.small,
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val strokeWidth = BuildNotifyTheme.dimensions.stroke.regular
    val clickableModifier = if (enabled) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .heightIn(BuildNotifyTheme.dimensions.component.buttonMinHeight)
                .graphicsLayer { alpha = if (enabled) 1f else DisabledAlpha }
                .border(width = strokeWidth, color = borderColor, shape = shape)
                .clip(shape)
                .then(clickableModifier)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
        )
    }
}
