package me.yuriisoft.buildnotify.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.DarkBrushScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.LightBrushScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.color.DarkColorScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.color.LightColorScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.color.toMaterialColors
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.BuildNotifyDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.CompactDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.shapes.DefaultShapes
import me.yuriisoft.buildnotify.mobile.ui.theme.typography.buildTypography
import me.yuriisoft.buildnotify.mobile.ui.theme.typography.rememberBuildNotifyTypography

@Composable
fun BuildNotifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dimensions: BuildNotifyDimensions = CompactDimensions,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val brushes = if (darkTheme) DarkBrushScheme else LightBrushScheme
    val typography = rememberBuildNotifyTypography()

    CompositionLocalProvider(
        LocalBuildNotifyColors provides scheme,
        LocalBuildNotifyShapes provides DefaultShapes,
        LocalBuildNotifyTypography provides typography,
        LocalBuildNotifyBrushes provides brushes,
        LocalBuildNotifyDimensions provides dimensions,
    ) {
        MaterialTheme(
            colors = scheme.toMaterialColors(),
            content = content,
        )
    }
}

val LocalBuildNotifyColors = staticCompositionLocalOf { LightColorScheme }
val LocalBuildNotifyShapes = staticCompositionLocalOf { DefaultShapes }
val LocalBuildNotifyTypography = staticCompositionLocalOf { buildTypography() }
val LocalBuildNotifyBrushes = staticCompositionLocalOf { LightBrushScheme }
val LocalBuildNotifyDimensions = staticCompositionLocalOf { CompactDimensions }
