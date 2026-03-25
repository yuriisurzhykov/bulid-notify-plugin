package me.yuriisoft.buildnotify.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.BrushScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.color.ColorScheme
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.BuildNotifyDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.shapes.BuildNotifyShapes
import me.yuriisoft.buildnotify.mobile.ui.theme.typography.BuildNotifyTypography

object BuildNotifyTheme {
    val colors: ColorScheme
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

    val dimensions: BuildNotifyDimensions
        @Composable @ReadOnlyComposable
        get() = LocalBuildNotifyDimensions.current
}
