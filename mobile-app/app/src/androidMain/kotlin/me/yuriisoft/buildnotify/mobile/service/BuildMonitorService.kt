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
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.ConnectionStatus
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.repository.IConnectionRepository

/**
 * Foreground service that keeps the WebSocket connection alive and posts
 * build-result notifications while the app is in the background.
 *
 * Lifecycle is driven by [ConnectionServiceManager]:
 *   - Started when [ConnectionStatus.Connected] is observed.
 *   - Stops itself when the connection transitions to [ConnectionStatus.Disconnected].
 *
 * Dependencies are retrieved from the application-scoped [AppComponent]
 * because Android [Service] instances cannot use constructor injection.
 */
class BuildMonitorService : Service() {

    private lateinit var connectionRepo: IConnectionRepository
    private lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val component = (application as BuildNotifyApp).component
        connectionRepo = component.connectionRepository
        notificationHelper = NotificationHelper(this)

        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            notificationHelper.persistentNotification(),
        )

        observeConnectionStatus()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeConnectionStatus() {
        serviceScope.launch {
            connectionRepo.status.collect { status ->
                when (status) {
                    is ConnectionStatus.Connected ->
                        notificationHelper.updatePersistent("Connected to ${status.host.name}")

                    is ConnectionStatus.Connecting ->
                        notificationHelper.updatePersistent("Connecting to ${status.host.name}…")

                    is ConnectionStatus.Error ->
                        notificationHelper.updatePersistent("Connection lost. Reconnecting…")

                    is ConnectionStatus.Disconnected -> stopSelf()
                }
            }
        }
    }
}
