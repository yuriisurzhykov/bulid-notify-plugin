package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Stable
fun interface CircularProgressDrawer {
    fun DrawScope.draw(gradient: GradientSpec, trackColor: Color, stroke: Stroke)
}