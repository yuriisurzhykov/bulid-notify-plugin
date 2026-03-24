package me.yuriisoft.buildnotify.mobile.ui.theme.shapes

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class BuildNotifyShapes(
    val card: Shape = RoundedCornerShape(16.dp),
    val statsCard: Shape = RoundedCornerShape(12.dp),
    val buttonFilled: Shape = RoundedCornerShape(12.dp),
    val buttonOutline: Shape = RoundedCornerShape(8.dp),
    val codeBlock: Shape = RoundedCornerShape(8.dp),
    val progressBar: Shape = RoundedCornerShape(percent = 50),
    val badge: Shape = RoundedCornerShape(percent = 50),
)
