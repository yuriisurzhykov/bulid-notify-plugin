package me.yuriisoft.buildnotify.mobile.ui.theme.shapes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class BuildNotifyShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val full: Shape,
)

val DefaultShapes = BuildNotifyShapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    full = RoundedCornerShape(percent = 50),
)
