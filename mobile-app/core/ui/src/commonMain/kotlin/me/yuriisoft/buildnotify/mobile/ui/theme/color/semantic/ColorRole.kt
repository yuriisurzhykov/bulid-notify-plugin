package me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorRole(
    val main: Color,
    val variant: Color,
    val onMain: Color,
    val container: Color,
    val onContainer: Color,
)