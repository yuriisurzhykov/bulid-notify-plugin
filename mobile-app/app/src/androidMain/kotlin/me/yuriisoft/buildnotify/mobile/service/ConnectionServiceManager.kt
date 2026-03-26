package me.yuriisoft.buildnotify.mobile.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionState

/**
 * Bridges the [ConnectionManager] with the Android [BuildMonitorService]
 * foreground service.
 *
 * Call [bind] once from [MainActivity.onCreate] with the activity's lifecycle scope.
 * The manager then automatically:
 *   - Starts the foreground service when a WebSocket connection is established.
 *   - Stops the foreground service when the connection is fully disconnected.
 *
 * Intermediate states ([ConnectionState.Connecting], [ConnectionState.Reconnecting])
 * are intentionally ignored — the service handles reconnection UI via its own
 * state observer.
 */
class ConnectionServiceManager(
    private val context: Context,
    private val connectionManager: ConnectionManager,
) {

    fun bind(lifecycleScope: CoroutineScope) {
        lifecycleScope.launch {
            connectionManager.state.collect { state ->
                when (state) {
                    is ConnectionState.Connected    -> startService()
                    is ConnectionState.Disconnected -> stopService()
                    else                            -> Unit
                }
            }
        }
    }

    private fun startService() {
        val intent = Intent(context, BuildMonitorService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopService() {
        context.stopService(Intent(context, BuildMonitorService::class.java))
    }
}
