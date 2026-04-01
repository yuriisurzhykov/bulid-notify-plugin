package me.yuriisoft.buildnotify.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import me.yuriisoft.buildnotify.mobile.R
import me.yuriisoft.buildnotify.mobile.data.protocol.BuildResult
import me.yuriisoft.buildnotify.mobile.service.NotificationHelper.Companion.CHANNEL_BUILD_EVENTS
import me.yuriisoft.buildnotify.mobile.service.NotificationHelper.Companion.CHANNEL_PERSISTENT

/**
 * Owns all notification channels and builds every [Notification] the app shows.
 *
 * Two channels:
 *   - [CHANNEL_PERSISTENT] — low-priority, ongoing notification for the foreground service.
 *   - [CHANNEL_BUILD_EVENTS] — high-priority heads-up notifications for build results.
 *
 * Instantiated by [BuildMonitorService]; never outlives the service.
 */
class NotificationHelper(private val context: Context) {

    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    fun persistentNotification(
        message: String = DEFAULT_PERSISTENT_TEXT,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(PERSISTENT_TITLE)
            .setContentText(message)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    fun updatePersistent(message: String) {
        manager.notify(NOTIFICATION_ID, persistentNotification(message))
    }

    fun showBuildSuccess(result: BuildResult) {
        val text = "${result.projectName} completed in ${result.durationMs / 1_000}s"
        val notification = buildEventNotification(
            title = "Build Succeeded",
            text = text,
            icon = android.R.drawable.ic_dialog_info,
        )
        manager.notify(result.buildId.hashCode(), notification)
    }

    fun showBuildFailure(result: BuildResult) {
        val text = buildString {
            append(result.projectName)
            append(" failed")
            if (result.errorCount > 0) {
                append(" with ${result.errorCount} error(s)")
            }
        }
        val notification = buildEventNotification(
            title = "Build Failed",
            text = text,
            icon = android.R.drawable.ic_dialog_alert,
        )
        manager.notify(result.buildId.hashCode(), notification)
    }

    private fun buildEventNotification(
        title: String,
        text: String,
        icon: Int,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_BUILD_EVENTS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

    private fun createChannels() {
        val persistent = NotificationChannel(
            CHANNEL_PERSISTENT,
            "Connection Status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing notification while connected to IDE"
            setShowBadge(false)
        }

        val buildEvents = NotificationChannel(
            CHANNEL_BUILD_EVENTS,
            "Build Events",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Build success and failure notifications"
        }

        manager.createNotificationChannels(listOf(persistent, buildEvents))
    }

    companion object {
        const val CHANNEL_PERSISTENT = "build_notify_persistent"
        const val CHANNEL_BUILD_EVENTS = "build_notify_events"
        const val NOTIFICATION_ID = 1_001
        private const val PERSISTENT_TITLE = "Build Notify"
        private const val DEFAULT_PERSISTENT_TEXT = "Waiting for connection…"
    }
}
