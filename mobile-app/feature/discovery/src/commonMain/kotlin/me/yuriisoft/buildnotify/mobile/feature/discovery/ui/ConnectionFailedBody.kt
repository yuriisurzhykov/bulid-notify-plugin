package me.yuriisoft.buildnotify.mobile.feature.discovery.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.action_try_again
import build_notify_mobile.feature.discovery.generated.resources.connection_failed_title
import me.yuriisoft.buildnotify.mobile.ui.components.button.SecondaryButton
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Divider
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.BodyIcon
import me.yuriisoft.buildnotify.mobile.ui.icons.CloseIcon
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.TextResource
import me.yuriisoft.buildnotify.mobile.ui.resource.textResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun ConnectionFailedBody(
    host: TextResource,
    reason: TextResource,
    onRetry: () -> Unit,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val error = BuildNotifyTheme.colors.status.error
    val closeIcon = ImageResource.VectorImage(CloseIcon)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BodyIcon(
                containerColor = error.container,
                contentColor = error.onContainer,
                image = closeIcon
            )

            Spacer(Modifier.height(spacing.large))

            Text(
                text = textResource(Res.string.connection_failed_title),
                style = BuildNotifyTheme.typography.titleMedium,
                color = error.main,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.tiny))

            Text(
                text = host,
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(spacing.xxSmall))

            Text(
                text = reason,
                style = BuildNotifyTheme.typography.bodySmall,
                color = BuildNotifyTheme.colors.content.secondary,
                textAlign = TextAlign.Center,
            )

            Divider(
                Modifier.padding(
                    horizontal = BuildNotifyTheme.dimensions.spacing.small,
                    vertical = BuildNotifyTheme.dimensions.spacing.large,
                )
            )

            SecondaryButton(onClick = onRetry) {
                Text(
                    text = textResource(Res.string.action_try_again),
                    style = BuildNotifyTheme.typography.labelLarge,
                )
            }
        }
    }
}
