package me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class DiffColors(
    val addBackground: Color,
    val addContent: Color,
    val removeBackground: Color,
    val removeContent: Color,
)