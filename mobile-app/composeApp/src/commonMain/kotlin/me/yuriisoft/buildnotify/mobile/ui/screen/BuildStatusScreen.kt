package me.yuriisoft.buildnotify.mobile.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.resource.RawText

/**
 * Shows live build status streamed from the plugin over WebSocket.
 *
 * Full implementation in Phase 4:
 *   - BuildStatusViewModel (connected to KtorWebSocketClient via IBuildRepository)
 *   - Real-time task list with status indicators
 *   - Diagnostic messages (errors / warnings) with source file references
 *   - Navigation to HistoryScreen when the build completes
 */
@Composable
fun BuildStatusScreen(
    host: String,
    port: Int,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = RawText("Build Status [$host:$port] — Phase 4"))
    }
}
