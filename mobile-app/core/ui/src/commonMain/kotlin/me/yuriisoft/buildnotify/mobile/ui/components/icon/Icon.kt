package me.yuriisoft.buildnotify.mobile.ui.components.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource

@NonRestartableComposable
@Composable
fun Icon(
    image: ImageResource,
    contentDescription: TextResource?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val stableImage = image.resolve()
    val painter = stableImage.createPainter()
    val colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint)
    val description = contentDescription?.resolve()?.toString()
    val semantics = if (description != null) {
        Modifier.semantics {
            this.contentDescription = description
            this.role = Role.Image
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .defaultIconSize(painter)
            .paint(painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
            .then(semantics),
    )
}

private fun Modifier.defaultIconSize(painter: Painter): Modifier =
    if (painter.intrinsicSize == Size.Unspecified ||
        (painter.intrinsicSize.width.isInfinite() && painter.intrinsicSize.height.isInfinite())
    ) {
        then(Modifier.size(24.dp))
    } else {
        this
    }
