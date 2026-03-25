package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Immutable
data class RingData(
    val progress: Float,
    val gradient: GradientSpec,
)