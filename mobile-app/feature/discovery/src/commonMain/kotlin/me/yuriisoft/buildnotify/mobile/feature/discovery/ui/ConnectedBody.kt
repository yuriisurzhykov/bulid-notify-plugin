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
import build_notify_mobile.feature.discovery.generated.resources.connected_title
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.BodyIcon
import me.yuriisoft.buildnotify.mobile.ui.icons.CheckIcon
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.textResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun ConnectedBody(host: DiscoveredHost) {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val success = BuildNotifyTheme.colors.status.success
    val checkIcon = ImageResource.VectorImage(CheckIcon)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BodyIcon(
                containerColor = success.container,
                contentColor = success.onContainer,
                image = checkIcon,
            )

            Spacer(Modifier.height(spacing.large))

            Text(
                text = textResource(Res.string.connected_title, host.name),
                style = BuildNotifyTheme.typography.titleMedium,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.tiny))

            Text(
                text = textResource("${host.host}:${host.port}"),
                style = BuildNotifyTheme.typography.bodySmall,
                color = BuildNotifyTheme.colors.content.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
