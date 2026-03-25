package me.yuriisoft.buildnotify.mobile.feature.discovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.StatusDot
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.layout.Surface
import me.yuriisoft.buildnotify.mobile.ui.components.progress.CircularProgress
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular.CircularProgressMode
import me.yuriisoft.buildnotify.mobile.ui.resource.RawText
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
internal fun DiscoveryContent(
    state: DiscoveryUiState,
    onHostSelected: (DiscoveredHost) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing
    val layout = BuildNotifyTheme.dimensions.layout

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = layout.contentPadding),
    ) {
        Spacer(Modifier.height(spacing.xxLarge))

        Text(
            text = RawText("Discovery"),
            style = BuildNotifyTheme.typography.headingLarge,
            color = BuildNotifyTheme.colors.content.primary,
        )

        Spacer(Modifier.height(spacing.tiny))

        Text(
            text = RawText("Find Build Notify instances on your network"),
            style = BuildNotifyTheme.typography.bodyMedium,
            color = BuildNotifyTheme.colors.content.secondary,
        )

        Spacer(Modifier.height(spacing.xLarge))

        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
        ) { currentState ->
            when (currentState) {
                is DiscoveryUiState.Loading -> LoadingBody()
                is DiscoveryUiState.Content -> HostListBody(currentState.hosts, onHostSelected)
                is DiscoveryUiState.Error -> ErrorBody(currentState.message)
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    val spacing = BuildNotifyTheme.dimensions.spacing

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgress(mode = CircularProgressMode.Indeterminate())

            Spacer(Modifier.height(spacing.regular))

            Text(
                text = RawText("Searching for devices\u2026"),
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.secondary,
            )
        }
    }
}

@Composable
private fun HostListBody(
    hosts: List<DiscoveredHost>,
    onHostSelected: (DiscoveredHost) -> Unit,
) {
    if (hosts.isEmpty()) {
        EmptyBody()
        return
    }

    val spacing = BuildNotifyTheme.dimensions.spacing

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        items(
            items = hosts,
            key = { "${it.host}:${it.port}" },
        ) { host ->
            HostItem(host = host, onClick = { onHostSelected(host) })
        }
    }
}

@Composable
private fun HostItem(
    host: DiscoveredHost,
    onClick: () -> Unit,
) {
    val spacing = BuildNotifyTheme.dimensions.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = BuildNotifyTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.regular),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = BuildNotifyTheme.colors.status.success.main)

            Spacer(Modifier.width(spacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = RawText(host.name),
                    style = BuildNotifyTheme.typography.titleMedium,
                    color = BuildNotifyTheme.colors.content.primary,
                    maxLines = 1,
                )

                Spacer(Modifier.height(spacing.xxSmall))

                Text(
                    text = RawText("${host.host}:${host.port}"),
                    style = BuildNotifyTheme.typography.bodySmall,
                    color = BuildNotifyTheme.colors.content.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyBody() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = RawText("No devices found"),
                style = BuildNotifyTheme.typography.titleMedium,
                color = BuildNotifyTheme.colors.content.primary,
            )

            Spacer(Modifier.height(BuildNotifyTheme.dimensions.spacing.small))

            Text(
                text = RawText("Make sure the Build Notify plugin is running in your IDE"),
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.secondary,
            )
        }
    }
}

@Composable
private fun ErrorBody(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = RawText("Something went wrong"),
                style = BuildNotifyTheme.typography.titleMedium,
                color = BuildNotifyTheme.colors.status.error.main,
            )

            Spacer(Modifier.height(BuildNotifyTheme.dimensions.spacing.small))

            Text(
                text = RawText(message),
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.secondary,
            )
        }
    }
}
