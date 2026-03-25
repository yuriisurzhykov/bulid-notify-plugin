package me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import me.yuriisoft.buildnotify.mobile.ui.theme.brush.semantic.GradientSpec

@Stable
fun interface LinearProgressDrawer {
    fun DrawScope.draw(gradient: GradientSpec, trackColor: Color)
}