package me.yuriisoft.buildnotify.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.BrushScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.DarkBrushScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.LightBrushScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.shapes.BuildNotifyShapes
import me.yuriisoft.buildnotify.mobile.ui.theme.shapes.DefaultShapes
import me.yuriisoft.buildnotify.mobile.ui.theme.typography.BuildNotifyTypography

val LocalBuildNotifyColors = staticCompositionLocalOf { LightColorScheme }
val LocalBuildNotifyShapes = staticCompositionLocalOf { DefaultShapes }
val LocalBuildNotifyTypography = staticCompositionLocalOf { BuildNotifyTypography() }
val LocalBuildNotifyBrushes = staticCompositionLocalOf { LightBrushScheme }

@Composable
fun BuildNotifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val brushes = if (darkTheme) DarkBrushScheme else LightBrushScheme

    CompositionLocalProvider(
        LocalBuildNotifyColors provides scheme,
        LocalBuildNotifyShapes provides DefaultShapes,
        LocalBuildNotifyTypography provides BuildNotifyTypography(),
        LocalBuildNotifyBrushes provides brushes,
    ) {
        MaterialTheme(
            colors = scheme.toMaterialColors(),
            content = content,
        )
    }
}

object BuildNotifyTheme {
    val colors: BuildNotifyColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalBuildNotifyColors.current

    val shapes: BuildNotifyShapes
        @Composable @ReadOnlyComposable
        get() = LocalBuildNotifyShapes.current

    val typography: BuildNotifyTypography
        @Composable @ReadOnlyComposable
        get() = LocalBuildNotifyTypography.current

    val brushes: BrushScheme
        @Composable @ReadOnlyComposable
        get() = LocalBuildNotifyBrushes.current
}
