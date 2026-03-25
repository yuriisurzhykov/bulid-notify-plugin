package me.yuriisoft.buildnotify.mobile.ui.theme.brush

import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Immutable
data class BrushScheme(
    val actionGradient: GradientSpec,
    val progressGradient: GradientSpec,
)