package me.yuriisoft.buildnotify.mobile.ui.resource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
interface ImageResource {
    @Composable
    fun resolve(): StableImage
}

@Immutable
data class VectorImage(val imageVector: ImageVector) : ImageResource {
    @Composable
    override fun resolve(): StableImage = VectorStableImage(imageVector)
}

@Immutable
data class ResImage(val resource: DrawableResource) : ImageResource {
    @Composable
    override fun resolve(): StableImage {
        val painter = painterResource(resource)
        return remember(painter) { PainterStableImage(painter) }
    }
}
