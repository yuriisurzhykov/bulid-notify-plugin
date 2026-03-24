package me.yuriisoft.buildnotify.mobile.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Discovery — Phase 4")
    }
}
