package me.yuriisoft.buildnotify.mobile.feature.discovery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.network_unavailable_body
import build_notify_mobile.feature.discovery.generated.resources.network_unavailable_title
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.BodyIcon
import me.yuriisoft.buildnotify.mobile.ui.icons.InfoIcon
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.textResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun NetworkUnavailableBody() {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val warning = BuildNotifyTheme.colors.status.warning
    val infoIcon = ImageResource.VectorImage(InfoIcon)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BodyIcon(
                containerColor = warning.container,
                contentColor = warning.onContainer,
                image = infoIcon,
            )

            Spacer(Modifier.height(spacing.large))

            Text(
                text = textResource(Res.string.network_unavailable_title),
                style = BuildNotifyTheme.typography.titleMedium,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.tiny))

            Text(
                text = textResource(Res.string.network_unavailable_body),
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
