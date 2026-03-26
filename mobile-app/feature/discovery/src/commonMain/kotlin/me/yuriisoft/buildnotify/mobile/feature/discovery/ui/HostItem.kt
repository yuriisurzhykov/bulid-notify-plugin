package me.yuriisoft.buildnotify.mobile.feature.discovery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.StatusDot
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.layout.Surface
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun HostItem(
    host: DiscoveredHost,
    onClick: () -> Unit,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = BuildNotifyTheme.shapes.medium,
        elevation = BuildNotifyTheme.dimensions.elevation.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.regular),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = BuildNotifyTheme.colors.status.success.main)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(spacing.small)
            ) {
                Text(
                    text = TextResource.RawText(host.name),
                    style = BuildNotifyTheme.typography.titleMedium,
                    color = BuildNotifyTheme.colors.content.primary,
                    maxLines = 1,
                )

                Text(
                    modifier = Modifier.padding(spacing.xxSmall),
                    text = TextResource.RawText("${host.host}:${host.port}"),
                    style = BuildNotifyTheme.typography.bodySmall,
                    color = BuildNotifyTheme.colors.content.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}