package me.yuriisoft.buildnotify.mobile.feature.discovery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import build_notify_mobile.feature.discovery.generated.resources.Res
import build_notify_mobile.feature.discovery.generated.resources.discovery_subtitle
import build_notify_mobile.feature.discovery.generated.resources.discovery_title
import build_notify_mobile.feature.discovery.generated.resources.discovery_version_footer
import me.yuriisoft.buildnotify.mobile.feature.discovery.presentation.DiscoveryUiState
import me.yuriisoft.buildnotify.mobile.network.connection.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.StatusIcon
import me.yuriisoft.buildnotify.mobile.ui.components.layout.FlatRow
import me.yuriisoft.buildnotify.mobile.ui.components.layout.FlatRowSlot
import me.yuriisoft.buildnotify.mobile.ui.resource.ImageResource
import me.yuriisoft.buildnotify.mobile.ui.resource.textResource
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

private val DiscoveryHeaderSlots = listOf(
    FlatRowSlot(0.15f),
    FlatRowSlot(0.85f),
)

@Composable
internal fun DiscoveryContent(
    state: DiscoveryUiState,
    appVersion: String,
    onHostSelected: (DiscoveredHost) -> Unit,
    onConfirmPairing: () -> Unit,
    onRejectPairing: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val layout = BuildNotifyTheme.dimensions.layout

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = layout.contentPadding),
        ) {
            Spacer(Modifier.height(spacing.xxLarge))

            FlatRow(
                modifier = Modifier.fillMaxWidth(),
                slots = DiscoveryHeaderSlots,
                horizontalSpacing = BuildNotifyTheme.dimensions.spacing.regular,
                verticalSpacing = BuildNotifyTheme.dimensions.spacing.regular,
            ) {
                StatusIcon(
                    modifier = Modifier.slot(0),
                    containerColor = BuildNotifyTheme.colors.primary.container,
                    contentColor = BuildNotifyTheme.colors.primary.onContainer,
                    image = ImageResource.VectorImage(HubIcon),
                )

                Text(
                    modifier = Modifier.slot(1),
                    text = textResource(Res.string.discovery_title),
                    style = BuildNotifyTheme.typography.displayLarge,
                    color = BuildNotifyTheme.colors.content.primary,
                    textAlign = TextAlign.Start,
                )

                Text(
                    modifier = Modifier.slot(1),
                    text = textResource(Res.string.discovery_subtitle),
                    style = BuildNotifyTheme.typography.bodyMedium,
                    color = BuildNotifyTheme.colors.content.secondary,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(Modifier.height(spacing.xLarge))

            AnimatedContent(
                targetState = state,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val isForward = targetState.animateOrder > initialState.animateOrder
                    val isBackward = targetState.animateOrder < initialState.animateOrder
                    val isFade = targetState.animateOrder == initialState.animateOrder ||
                            targetState.animateOrder < 0
                    when {
                        isForward  -> slideInHorizontally { fullWidth -> fullWidth } togetherWith slideOutHorizontally { fullWidth -> -fullWidth }
                        isBackward -> slideInHorizontally { fullWidth -> -fullWidth } togetherWith slideOutHorizontally { fullWidth -> fullWidth }
                        isFade     -> fadeIn() togetherWith fadeOut()
                        else       -> fadeIn() togetherWith fadeOut()
                    }
                },
                contentKey = { it::class.qualifiedName },
            ) { currentState ->
                when (currentState) {
                    is DiscoveryUiState.Idle                -> IdleBody(onStartScan = onStartScan)
                    is DiscoveryUiState.Scanning            -> ScanningBody(onCancel = onCancel)
                    is DiscoveryUiState.ServiceSelection    -> HostListBody(
                        currentState.hosts,
                        onHostSelected,
                    )

                    is DiscoveryUiState.PairingConfirmation -> PairingConfirmationBody(
                        host = currentState.host,
                        fingerprint = currentState.fingerprint,
                        onConfirm = onConfirmPairing,
                        onReject = onRejectPairing,
                    )

                    is DiscoveryUiState.Connecting          -> ConnectingBody(
                        host = currentState.host,
                        onCancel = onCancel,
                    )

                    is DiscoveryUiState.Connected           -> ConnectedBody(currentState.host)
                    is DiscoveryUiState.ConnectionFailed    -> ConnectionFailedBody(
                        host = currentState.hostResource,
                        reason = currentState.reasonResource,
                        onRetry = { onHostSelected(currentState.host) },
                    )

                    is DiscoveryUiState.NothingFound        -> EmptyBody(onRetry = onRetry)
                    is DiscoveryUiState.ScanError           -> ErrorBody(
                        message = currentState.message,
                        onRetry = onRetry,
                    )

                    is DiscoveryUiState.NetworkUnavailable  -> NetworkUnavailableBody()
                }
            }

            Text(
                text = textResource(Res.string.discovery_version_footer, appVersion),
                style = BuildNotifyTheme.typography.bodySmall,
                color = BuildNotifyTheme.colors.content.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.regular),
            )
        }
    }
}
