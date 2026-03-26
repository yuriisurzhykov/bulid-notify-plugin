package me.yuriisoft.buildnotify.mobile.feature.discovery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.action_cancel
import build_notify_mobile.feature.discovery.generated.resources.connecting_title
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.components.button.GhostButton
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Divider
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.StatusIcon
import me.yuriisoft.buildnotify.mobile.ui.components.progress.DotProgress
import me.yuriisoft.buildnotify.mobile.ui.components.progress.RingProgress
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.dot.DotProgressMode
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.ring.RingProgressMode
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun ConnectingBody(
    host: DiscoveredHost,
    onCancel: () -> Unit,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val primary = BuildNotifyTheme.colors.primary

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                RingProgress(mode = RingProgressMode.Pulsing())
                StatusIcon(
                    containerColor = primary.container,
                    contentColor = primary.onContainer,
                    image = ImageResource.VectorImage(LinkIcon),
                )
            }

            Spacer(Modifier.height(spacing.large))

            Text(
                text = TextResource.ResText(Res.string.connecting_title, listOf(host.name)),
                style = BuildNotifyTheme.typography.titleMedium,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.tiny))

            Text(
                text = TextResource.RawText("${host.host}:${host.port}"),
                style = BuildNotifyTheme.typography.bodySmall,
                color = BuildNotifyTheme.colors.content.tertiary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.regular))

            DotProgress(
                mode = DotProgressMode.Bouncing(),
                modifier = Modifier.size(
                    width = BuildNotifyTheme.dimensions.component.ringProgressSize,
                    height = BuildNotifyTheme.dimensions.component.dotProgressDotSize * 3,
                ),
            )

            Divider(
                Modifier.padding(
                    horizontal = BuildNotifyTheme.dimensions.spacing.small,
                    vertical = BuildNotifyTheme.dimensions.spacing.large,
                )
            )

            GhostButton(onClick = onCancel) {
                Text(
                    text = TextResource.ResText(Res.string.action_cancel),
                    style = BuildNotifyTheme.typography.labelLarge,
                )
            }
        }
    }
}
