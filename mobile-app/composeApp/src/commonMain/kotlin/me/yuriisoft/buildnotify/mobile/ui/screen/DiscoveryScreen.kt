package me.yuriisoft.buildnotify.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

/**
 * Lists Build Notify plugin instances discovered on the local network via mDNS.
 *
 * Full implementation in Phase 4:
 *   - DiscoveryViewModel (kotlin-inject + lifecycle-viewmodel)
 *   - LazyColumn of DiscoveredHost cards with connection state
 *   - Pull-to-refresh / manual re-scan action
 */
@Composable
fun DiscoveryScreen(
    onHostSelected: (host: String, port: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BuildNotifyTheme.colors.surface.primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Discovery — Phase 4", color = BuildNotifyTheme.colors.content.primary)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(65.dp)
                .background(
                    brush = BuildNotifyTheme.brushes.actionGradient.toBrush(),
                    shape = BuildNotifyTheme.shapes.medium,
                )
        )
    }
}
