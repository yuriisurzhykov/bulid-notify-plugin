package me.yuriisoft.buildnotify.mobile.feature.discovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.pairing_body
import build_notify_mobile.feature.discovery.generated.resources.pairing_confirm
import build_notify_mobile.feature.discovery.generated.resources.pairing_fingerprint_label
import build_notify_mobile.feature.discovery.generated.resources.pairing_reject
import build_notify_mobile.feature.discovery.generated.resources.pairing_title
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.components.button.GhostButton
import me.yuriisoft.buildnotify.mobile.ui.components.button.PrimaryButton
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Divider
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.BodyIcon
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.resource.textResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun PairingConfirmationBody(
    host: DiscoveredHost,
    fingerprint: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val warning = BuildNotifyTheme.colors.status.warning

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BodyIcon(
                containerColor = warning.container,
                contentColor = warning.onContainer,
                image = ImageResource.VectorImage(ShieldIcon),
            )

            Spacer(Modifier.height(spacing.large))

            Text(
                text = textResource(Res.string.pairing_title),
                style = BuildNotifyTheme.typography.titleMedium,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.tiny))

            Text(
                text = TextResource.RawText(host.name),
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.xxSmall))

            Text(
                text = textResource(Res.string.pairing_body),
                style = BuildNotifyTheme.typography.bodySmall,
                color = BuildNotifyTheme.colors.content.secondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.large))

            Text(
                text = textResource(Res.string.pairing_fingerprint_label),
                style = BuildNotifyTheme.typography.labelSmall,
                color = BuildNotifyTheme.colors.content.tertiary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.xxSmall))

            Text(
                text = TextResource.RawText(fingerprint),
                style = BuildNotifyTheme.typography.bodySmall,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.regular),
            )

            Divider(
                Modifier.padding(
                    horizontal = spacing.small,
                    vertical = spacing.large,
                )
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GhostButton(onClick = onReject) {
                    Text(
                        text = textResource(Res.string.pairing_reject),
                        style = BuildNotifyTheme.typography.labelLarge,
                    )
                }

                Spacer(Modifier.width(spacing.regular))

                PrimaryButton(onClick = onConfirm) {
                    Text(
                        text = textResource(Res.string.pairing_confirm),
                        style = BuildNotifyTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
