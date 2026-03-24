package me.yuriisoft.buildnotify.mobile.ui.theme.brush

import me.yuriisoft.buildnotify.mobile.ui.theme.Palette
import me.yuriisoft.buildnotify.mobile.ui.theme.gradient.GradientSpec

val LightBrushScheme = BrushScheme(
    actionGradient = GradientSpec.LinearGradientSpec(
        colors = listOf(Palette.Blue500, Palette.Violet500),
        angle = 45f,
    ),
    progressGradient = GradientSpec.LinearGradientSpec(
        colors = listOf(Palette.Green500, Palette.Lime500),
        angle = 0f,
    ),
)

val DarkBrushScheme = BrushScheme(
    actionGradient = GradientSpec.LinearGradientSpec(
        colors = listOf(Palette.Blue500, Palette.Violet500),
        angle = 45f,
    ),
    progressGradient = GradientSpec.LinearGradientSpec(
        colors = listOf(Palette.Green500, Palette.Lime500),
        angle = 0f,
    ),
)