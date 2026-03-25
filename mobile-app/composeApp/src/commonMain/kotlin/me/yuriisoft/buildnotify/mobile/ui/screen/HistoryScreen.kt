package me.yuriisoft.buildnotify.mobile.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.resource.RawText

/**
 * Displays past build results stored locally.
 *
 * Full implementation in Phase 4:
 *   - HistoryViewModel backed by a local Room (Android) / SQLDelight store
 *   - LazyColumn of BuildResult cards grouped by date
 *   - Tap to expand individual task / diagnostic details
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = RawText("History — Phase 4"))
    }
}
