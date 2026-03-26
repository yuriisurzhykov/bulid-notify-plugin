package me.yuriisoft.buildnotify.mobile.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.yuriisoft.buildnotify.mobile.BuildNotifyApp
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionManager
import me.yuriisoft.buildnotify.mobile.network.connection.ConnectionState

/**
 * Foreground service that keeps the WebSocket connection alive and posts
 * build-result notifications while the app is in the background.
 *
 * Lifecycle is driven by [ConnectionServiceManager]:
 *   - Started when [ConnectionState.Connected] is observed.
 *   - Stops itself when the connection transitions to [ConnectionState.Disconnected].
 *
 * Dependencies are retrieved from the application-scoped [AppComponent]
 * because Android [Service] instances cannot use constructor injection.
 */
class BuildMonitorService : Service() {

    private lateinit var connectionManager: ConnectionManager
    private lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val component = (application as BuildNotifyApp).component
        connectionManager = component.connectionManager
        notificationHelper = NotificationHelper(this)

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            notificationHelper.persistentNotification(),
        )

        observeConnectionState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeConnectionState() {
        serviceScope.launch {
            connectionManager.state.collect { state ->
                when (state) {
                    is ConnectionState.Connected    -> notificationHelper.updatePersistent("Connected to ${state.host.name}")
                    is ConnectionState.Connecting   -> notificationHelper.updatePersistent("Connecting to ${state.host.name}…")
                    is ConnectionState.Reconnecting -> notificationHelper.updatePersistent("Reconnecting (attempt ${state.attempt})…")
                    is ConnectionState.Failed       -> notificationHelper.updatePersistent("Connection lost")
                    is ConnectionState.Disconnected -> stopSelf()
                    is ConnectionState.Idle         -> Unit
                }
            }
        }
    }
}
