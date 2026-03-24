package me.yuriisoft.buildnotify.mobile.ui.theme.gradient

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed interface GradientSpec {

    fun toBrush(): Brush

    @Immutable
    data class LinearGradientSpec(
        val colors: List<Color>,
        val angle: Float = 0f,
    ) : GradientSpec {
        override fun toBrush(): Brush {
            val rad = angle * PI.toFloat() / 180f
            val dx = cos(rad)
            val dy = sin(rad)
            val scale = 10_000f
            return Brush.linearGradient(
                colors = colors,
                start = Offset(scale * (0.5f - dx * 0.5f), scale * (0.5f - dy * 0.5f)),
                end = Offset(scale * (0.5f + dx * 0.5f), scale * (0.5f + dy * 0.5f)),
            )
        }
    }

    @Immutable
    data class RadialGradientSpec(
        val colors: List<Color>,
        val center: Offset = Offset.Unspecified,
        val radius: Float = Float.POSITIVE_INFINITY,
    ) : GradientSpec {
        override fun toBrush(): Brush {
            return Brush.radialGradient(
                colors = colors,
                center = center,
                radius = radius,
            )
        }
    }

    @Immutable
    data class SolidGradientSpec(
        val color: Color,
    ) : GradientSpec {
        override fun toBrush(): Brush {
            return SolidColor(color)
        }
    }
}