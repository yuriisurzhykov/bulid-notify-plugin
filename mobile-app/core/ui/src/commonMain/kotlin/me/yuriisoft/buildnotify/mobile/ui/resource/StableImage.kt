package me.yuriisoft.buildnotify.mobile.ui.resource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Stable
interface StableImage {
    @Composable
    fun createPainter(): Painter
}

@Immutable
data class VectorStableImage(val imageVector: ImageVector) : StableImage {
    @Composable
    override fun createPainter(): Painter = rememberVectorPainter(imageVector)
}

@Stable
internal class PainterStableImage(val painter: Painter) : StableImage {
    @Composable
    override fun createPainter(): Painter = painter

    override fun equals(other: Any?): Boolean =
        other is PainterStableImage && painter === other.painter

    override fun hashCode(): Int = painter.hashCode()
}
