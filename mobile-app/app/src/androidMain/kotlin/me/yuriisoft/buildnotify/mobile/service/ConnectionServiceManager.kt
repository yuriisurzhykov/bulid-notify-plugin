package me.yuriisoft.buildnotify.mobile.service

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionStatus
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.IConnectionRepository

/**
 * Bridges the domain-layer [IConnectionRepository] with the Android
 * [BuildMonitorService] foreground service.
 *
 * Call [bind] once from [MainActivity.onCreate] with the activity's lifecycle scope.
 * The manager then automatically:
 *   - Starts the foreground service when a WebSocket connection is established.
 *   - Stops the foreground service when the connection is fully disconnected.
 *
 * Intermediate states ([ConnectionStatus.Connecting], [ConnectionStatus.Error])
 * are intentionally ignored — the service handles reconnection UI via its own
 * status observer.
 */
class ConnectionServiceManager(
    private val context: Context,
    private val connectionRepo: IConnectionRepository,
) {

    fun bind(lifecycleScope: CoroutineScope) {
        lifecycleScope.launch {
            connectionRepo.status.collect { status ->
                when (status) {
                    is ConnectionStatus.Connected -> startService()
                    is ConnectionStatus.Disconnected -> stopService()
                    else -> Unit
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
